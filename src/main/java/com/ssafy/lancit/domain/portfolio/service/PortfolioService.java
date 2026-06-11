package com.ssafy.lancit.domain.portfolio.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.global.enums.FileParentType;

import lombok.RequiredArgsConstructor;

// 포트폴리오 CRUD - 프리랜서 전용
// 배너/결과물 파일은 FileService 에서 처리 (GCS + Redis Signed URL 캐시)
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final String DEFAULT_CATEGORY = "WEB_APP";
    private static final int MAX_SUMMARY_LENGTH = 30;
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "WEB_APP", "DESIGN", "BRANDING", "MARKETING", "PLANNING"
    );

    private final PortfolioMapper portfolioMapper;
    private final FileService fileService;

    // PORT-01 내 포트폴리오 목록 조회 (페이지네이션)
    public PageResponse<PortfolioDTO> getMyList(String email, PageRequest pageRequest) {
        // TODO 영은 [1]: List<PortfolioDTO> list = portfolioMapper.findByEmail(email, pageRequest)
        // TODO 영은 [2]: long total = portfolioMapper.countByEmail(email)
        // TODO 영은 [3]: return PageResponse.of(list, total, pageRequest)
        List<PortfolioDTO> list = portfolioMapper.findByEmail(email, pageRequest);
        long total = portfolioMapper.countByEmail(email);
        return PageResponse.of(list, total, pageRequest);
    }

    // CLI-SEAR-02 회사가 특정 프리랜서 공개 포트폴리오 조회 (페이지네이션)
    public PageResponse<PortfolioDTO> getPublicList(String email, PageRequest pageRequest) {
        // TODO 영은 [1]: List<PortfolioDTO> list = portfolioMapper.findPublicByEmail(email, pageRequest)
        // TODO 영은 [2]: long total = portfolioMapper.countPublicByEmail(email)
        // TODO 영은 [3]: return PageResponse.of(list, total, pageRequest)
        List<PortfolioDTO> list = portfolioMapper.findPublicByEmail(email, pageRequest);
        long total = portfolioMapper.countPublicByEmail(email);
        return PageResponse.of(list, total, pageRequest);
    }

 // PORT-02 포트폴리오 상세 조회 - Map 으로 조립해서 반환
    public Map<String, Object> getOne(int portfolioId) {
        PortfolioDTO dto = portfolioMapper.findById(portfolioId);
        if (dto == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        List<FileDTO> files = fileService.findByParent(FileParentType.PORTFOLIO_FILE, portfolioId);
        Map<String, Object> result = new HashMap<>();
        result.put("portfolio", dto);
        result.put("files", files);
        return result;
    }

    // PORT-03 포트폴리오 등록
    // 배너 이미지는 컨트롤러 호출 전 POST /api/files/upload 로 먼저 업로드
    @Transactional
    public void create(PortfolioDTO dto, String email) {
        validateForSave(dto);
        dto.setEmail(email);
        portfolioMapper.insert(dto);
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

        int updated = portfolioMapper.update(portfolioId, dto);
        if (updated == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    // PORT-04 포트폴리오 삭제 (@OwnerCheck 로 소유자 검증)
    // 정석: GCS + Redis 먼저 정리 → DB CASCADE 로 마무리
    @OwnerCheck(resourceType = "PORTFOLIO")
    @Transactional
    public void delete(int portfolioId) {
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

        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
