package com.ssafy.lancit.domain.portfolio.service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileUpdateRequest;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioSearchCondition;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioProfileMapper;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.PortfolioCategory;

import lombok.RequiredArgsConstructor;

// 포트폴리오 CRUD - 프리랜서 전용
// 배너/결과물 파일은 FileService 에서 처리 (GCS + Redis Signed URL 캐시)
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final String DEFAULT_CATEGORY = "WEB_APP";
    private static final int MAX_SUMMARY_LENGTH = 30;
    private static final int MAX_INTRO_LENGTH = 30;
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final PortfolioMapper portfolioMapper;
    private final PortfolioProfileMapper portfolioProfileMapper;
    private final FileService fileService;

    // PORT-01 내 포트폴리오 목록 조회 (페이지네이션)
    public PageResponse<PortfolioDTO> getMyList(String email, PageRequest pageRequest) {
        return getMyList(email, pageRequest, null);
    }

    // PORT-01 내 포트폴리오 목록 조회 (페이지네이션 + 검색/필터)
    public PageResponse<PortfolioDTO> getMyList(String email, PageRequest pageRequest,
                                                PortfolioSearchCondition condition) {
        PortfolioSearchCondition normalized = normalizeSearchCondition(condition);
        List<PortfolioDTO> list = portfolioMapper.findByEmail(email, pageRequest, normalized);
        long total = portfolioMapper.countByEmail(email, normalized);
        return PageResponse.of(list, total, pageRequest);
    }

    // CLI-SEAR-02 회사가 특정 프리랜서 공개 포트폴리오 조회 (페이지네이션)
    public PageResponse<PortfolioDTO> getPublicList(String email, PageRequest pageRequest) {
        return getPublicList(email, pageRequest, null);
    }

    // CLI-SEAR-02 회사가 특정 프리랜서 공개 포트폴리오 조회 (페이지네이션 + 검색/필터)
    public PageResponse<PortfolioDTO> getPublicList(String email, PageRequest pageRequest,
                                                    PortfolioSearchCondition condition) {
        PortfolioSearchCondition normalized = normalizeSearchCondition(condition);
        List<PortfolioDTO> list = portfolioMapper.findPublicByEmail(email, pageRequest, normalized);
        long total = portfolioMapper.countPublicByEmail(email, normalized);
        return PageResponse.of(list, total, pageRequest);
    }

    // PORT-PROFILE-01 내 포트폴리오 프로필 카드 조회
    @Transactional
    public PortfolioProfileDTO getMyProfile(String email) {
        PortfolioProfileDTO profile = ensureMyProfile(email);
        attachTechStacks(profile);
        return profile;
    }

    // PORT-PROFILE-02 내 포트폴리오 프로필 카드 저장
    @Transactional
    public PortfolioProfileDTO updateMyProfile(String email, PortfolioProfileUpdateRequest request) {
        if (request == null || !hasText(request.getDisplayName()) || request.getJobCategory() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        PortfolioProfileDTO existing = ensureMyProfile(email);

        if (request.getProfileFileId() != null
                && !request.getProfileFileId().equals(existing.getProfileFileId())) {
            fileService.promoteOwned(request.getProfileFileId(), FileParentType.PORTFOLIO_PROFILE, email);
        }

        PortfolioProfileDTO profile = PortfolioProfileDTO.builder()
                .freelancerEmail(email)
                .displayName(request.getDisplayName().trim())
                .jobCategory(request.getJobCategory())
                .profileFileId(request.getProfileFileId())
                .isPortfolioPublic(request != null && Boolean.TRUE.equals(request.getIsPortfolioPublic()))
                .intro(normalizeIntro(request.getIntro()))
                .description(normalizeDescription(request.getDescription()))
                .build();

        portfolioProfileMapper.updateProfile(profile);

        Integer oldProfileFileId = existing.getProfileFileId();
        if (oldProfileFileId != null && !oldProfileFileId.equals(request.getProfileFileId())) {
            fileService.deleteProfileIfUnreferenced(oldProfileFileId);
        }

        List<String> techStacks = normalizeTechStacks(request == null ? null : request.getTechStacks());
        portfolioProfileMapper.deleteTechStacks(email);
        for (String techStack : techStacks) {
            portfolioProfileMapper.insertTechStack(email, techStack);
        }

        return getMyProfile(email);
    }

 // PORT-02 포트폴리오 상세 조회 - Map 으로 조립해서 반환
    public Map<String, Object> getOne(int portfolioId) {
        PortfolioDTO dto = portfolioMapper.findById(portfolioId);
        if (dto == null) {
            throw new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }

        List<FileDTO> files = fileService.findByParent(FileParentType.PORTFOLIO_FILE, portfolioId);
        Map<String, Object> result = new HashMap<>();
        result.put("portfolio", dto);
        result.put("files", files);
        return result;
    }

    public Map<String, Object> getOneForViewer(int portfolioId, String viewerEmail) {
        PortfolioDTO portfolio = portfolioMapper.findById(portfolioId);
        if (portfolio == null) {
            throw new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }
        if (!viewerEmail.equals(portfolio.getEmail()) && !Boolean.TRUE.equals(portfolio.getIsPublic())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return getOne(portfolioId);
    }

    // PORT-03 포트폴리오 등록
    // 배너 이미지는 컨트롤러 호출 전 POST /api/files/upload 로 먼저 업로드
    @Transactional
    public Integer create(PortfolioDTO dto, String email) {
        validateForSave(dto);
        dto.setEmail(email);
        portfolioMapper.insert(dto);
        if (dto.getBannerFileId() != null) {
            fileService.attachToParent(
                    dto.getBannerFileId(),
                    FileParentType.PORTFOLIO_BANNER,
                    dto.getPortfolioId(),
                    email);
        }
        return dto.getPortfolioId();
    }

    // PORT-03 포트폴리오 수정 (@OwnerCheck 로 소유자 검증)
    @OwnerCheck(resourceType = "PORTFOLIO")
    @Transactional
    public void update(int portfolioId, PortfolioDTO dto) {
        validateForSave(dto);
        PortfolioDTO existing = portfolioMapper.findById(portfolioId);
        if (existing == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        Integer oldBannerFileId = existing.getBannerFileId();
        Integer newBannerFileId = dto.getBannerFileId();
        boolean bannerChanged = !Objects.equals(oldBannerFileId, newBannerFileId);
        if (newBannerFileId != null) {
            fileService.attachToParent(
                    newBannerFileId,
                    FileParentType.PORTFOLIO_BANNER,
                    portfolioId,
                    existing.getEmail());
        }

        int updated = portfolioMapper.update(portfolioId, dto);
        if (updated == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        if (bannerChanged && oldBannerFileId != null) {
            fileService.deletePortfolioFileIfUnreferenced(oldBannerFileId);
        }
    }

    // PORT-04 포트폴리오 삭제 (@OwnerCheck 로 소유자 검증)
    // 정석: GCS + Redis 먼저 정리 → DB CASCADE 로 마무리
    @OwnerCheck(resourceType = "PORTFOLIO")
    @Transactional
    public void delete(int portfolioId) {
        PortfolioDTO portfolio = portfolioMapper.findById(portfolioId);
        if (portfolio == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        if (portfolio.getBannerFileId() != null) {
            fileService.deleteBySystem(portfolio.getBannerFileId());
        }
        fileService.deleteByParent(FileParentType.PORTFOLIO_FILE, portfolioId);

        int deleted = portfolioMapper.softDelete(portfolioId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    private void validateForSave(PortfolioDTO dto) {
        if (dto == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (!hasText(dto.getTitle()) || !hasText(dto.getSummary())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getSummary().trim().length() > MAX_SUMMARY_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getWorkStartAt() != null && dto.getWorkEndAt() != null
                && dto.getWorkStartAt().isAfter(dto.getWorkEndAt())) {
            throw new CustomException(ErrorCode.INVALID_PORTFOLIO_PERIOD);
        }

        dto.setTitle(dto.getTitle().trim());
        dto.setSummary(dto.getSummary().trim());
        dto.setCategory(normalizeCategory(dto.getCategory()));

        if (dto.getIsPublic() == null) {
            dto.setIsPublic(false);
        }
    }

    private String normalizeCategory(String category) {
        if (!hasText(category)) {
            return DEFAULT_CATEGORY;
        }
        return PortfolioCategory.normalize(category);
    }

    private PortfolioSearchCondition normalizeSearchCondition(PortfolioSearchCondition condition) {
        if (condition == null) {
            return null;
        }

        condition.setKeyword(hasText(condition.getKeyword()) ? condition.getKeyword().trim() : null);
        condition.setCategory(hasText(condition.getCategory())
                ? PortfolioCategory.normalize(condition.getCategory())
                : null);

        if ("PUBLIC".equalsIgnoreCase(condition.getVisibility())) {
            condition.setVisibility("PUBLIC");
        } else if ("PRIVATE".equalsIgnoreCase(condition.getVisibility())) {
            condition.setVisibility("PRIVATE");
        } else {
            condition.setVisibility(null);
        }

        return condition;
    }

    private PortfolioProfileDTO ensureMyProfile(String email) {
        PortfolioProfileDTO profile = portfolioProfileMapper.findByFreelancerEmail(email);
        if (profile == null) {
            if (portfolioProfileMapper.insertProfileFromUser(email) == 0) {
                throw new CustomException(ErrorCode.NOT_FOUND);
            }
            profile = portfolioProfileMapper.findByFreelancerEmail(email);
        }

        return profile;
    }

    private void attachTechStacks(PortfolioProfileDTO profile) {
        profile.setTechStacks(portfolioProfileMapper.findTechStacks(profile.getFreelancerEmail()));
    }

    private String normalizeIntro(String intro) {
        if (!hasText(intro)) {
            return "";
        }
        String normalized = intro.trim();
        if (normalized.length() > MAX_INTRO_LENGTH) {
            throw new CustomException(ErrorCode.PORTFOLIO_PROFILE_INTRO_TOO_LONG);
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (!hasText(description)) {
            return "";
        }
        String normalized = description.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private List<String> normalizeTechStacks(List<String> techStacks) {
        if (techStacks == null || techStacks.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String techStack : techStacks) {
            if (!hasText(techStack)) {
                continue;
            }
            normalized.add(techStack.trim());
        }
        return List.copyOf(normalized);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    
    // PORT-PROFILE-01 회사가 포트폴리오 프로필 카드 조회
    @Transactional
    public PortfolioProfileDTO getFreelancerProfile(String email) {
    	PortfolioProfileDTO profile= portfolioProfileMapper.findByFreelancerEmail(email);
        return profile;
    }

}
