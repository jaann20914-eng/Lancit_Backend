package com.ssafy.lancit.domain.portfolio.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioSearchCondition;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.PortfolioCategory;

import lombok.RequiredArgsConstructor;

// 화면 용어는 프로젝트, 백엔드 도메인은 기존 구조에 맞춰 portfolio 를 사용한다.
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_COMPANY = "COMPANY";

    private final PortfolioMapper portfolioMapper;
    private final FileService fileService;

    public PageResponse<PortfolioDTO> getMyList(String email, String role, PortfolioSearchCondition condition) {
        requireFreelancer(role);
        SearchParams params = normalizeSearch(condition);

        List<PortfolioDTO> list = portfolioMapper.findByEmail(
                email,
                params.keyword(),
                params.visibility(),
                params.category(),
                params.sort(),
                params.offset(),
                params.size()
        );
        list.forEach(dto -> hydrateCard(dto, email));

        long total = portfolioMapper.countByEmail(
                email,
                params.keyword(),
                params.visibility(),
                params.category()
        );
        return PageResponse.of(list, total, params.pageRequest());
    }

    public PageResponse<PortfolioDTO> getPublicList(String freelancerEmail,
                                                    String currentEmail,
                                                    PortfolioSearchCondition condition) {
        SearchParams params = normalizeSearch(condition);
        List<PortfolioDTO> list = portfolioMapper.findPublicByEmail(
                freelancerEmail,
                params.keyword(),
                params.category(),
                params.sort(),
                params.offset(),
                params.size()
        );
        list.forEach(dto -> hydrateCard(dto, currentEmail));

        long total = portfolioMapper.countPublicByEmail(
                freelancerEmail,
                params.keyword(),
                params.category()
        );
        return PageResponse.of(list, total, params.pageRequest());
    }

    public PortfolioDTO getOne(int portfolioId, String currentEmail, String currentRole) {
        PortfolioDTO portfolio = findExistingPortfolio(portfolioId);
        validateReadable(portfolio, currentEmail, currentRole);
        return hydrateDetail(portfolio, currentEmail);
    }

    @Transactional
    public PortfolioDTO create(PortfolioDTO dto, String email, String role) {
        requireFreelancer(role);
        validateCreate(dto, email);

        List<Integer> fileIds = normalizeFileIds(dto.getFileIds());
        validateAttachableFile(dto.getBannerFileId(), email, FileParentType.PORTFOLIO_BANNER, null);
        for (Integer fileId : fileIds) {
            validateAttachableFile(fileId, email, FileParentType.PORTFOLIO_FILE, null);
        }

        dto.setEmail(email);
        dto.setCategory(PortfolioCategory.normalize(dto.getCategory()));
        portfolioMapper.insert(dto);

        attachBanner(dto.getBannerFileId(), email, dto.getPortfolioId());
        attachResultFiles(fileIds, email, dto.getPortfolioId());

        PortfolioDTO created = portfolioMapper.findById(dto.getPortfolioId());
        return hydrateDetail(created == null ? dto : created, email);
    }

    @Transactional
    public PortfolioDTO update(int portfolioId, PortfolioDTO dto, String email, String role) {
        requireFreelancer(role);
        PortfolioDTO existing = findExistingPortfolio(portfolioId);
        validateOwner(existing, email);
        validateUpdate(dto, existing);

        List<Integer> fileIds = dto.getFileIds() == null ? null : normalizeFileIds(dto.getFileIds());

        if (dto.getCategory() != null) {
            dto.setCategory(PortfolioCategory.normalize(dto.getCategory()));
        }
        if (dto.getBannerFileId() != null) {
            validateAttachableFile(dto.getBannerFileId(), email, FileParentType.PORTFOLIO_BANNER, portfolioId);
        }
        if (fileIds != null) {
            for (Integer fileId : fileIds) {
                validateAttachableFile(fileId, email, FileParentType.PORTFOLIO_FILE, portfolioId);
            }
        }

        int updatedCount = portfolioMapper.update(portfolioId, dto);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }

        if (dto.getBannerFileId() != null) {
            if (existing.getBannerFileId() != null && !Objects.equals(existing.getBannerFileId(), dto.getBannerFileId())) {
                fileService.detach(existing.getBannerFileId());
            }
            attachBanner(dto.getBannerFileId(), email, portfolioId);
        }

        if (fileIds != null) {
            fileService.detachByParent(FileParentType.PORTFOLIO_FILE, portfolioId);
            attachResultFiles(fileIds, email, portfolioId);
        }

        return getOne(portfolioId, email, role);
    }

    @Transactional
    public void delete(int portfolioId, String email, String role) {
        requireFreelancer(role);
        PortfolioDTO existing = findExistingPortfolio(portfolioId);
        validateOwner(existing, email);

        int deletedCount = portfolioMapper.softDelete(portfolioId);
        if (deletedCount == 0) {
            throw new CustomException(ErrorCode.DELETED_PORTFOLIO);
        }
    }

    private void validateCreate(PortfolioDTO dto, String email) {
        if (dto == null
                || isBlank(email)
                || isBlank(dto.getCategory())
                || isBlank(dto.getTitle())
                || isBlank(dto.getSummary())
                || isBlank(dto.getContent())
                || dto.getWorkStartAt() == null
                || dto.getWorkEndAt() == null
                || dto.getIsPublic() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        validatePeriod(dto.getWorkStartAt(), dto.getWorkEndAt());
    }

    private void validateUpdate(PortfolioDTO dto, PortfolioDTO existing) {
        if (dto == null || !hasUpdateField(dto)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getTitle() != null && isBlank(dto.getTitle())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getSummary() != null && isBlank(dto.getSummary())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getContent() != null && isBlank(dto.getContent())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        LocalDateTime start = dto.getWorkStartAt() == null ? existing.getWorkStartAt() : dto.getWorkStartAt();
        LocalDateTime end = dto.getWorkEndAt() == null ? existing.getWorkEndAt() : dto.getWorkEndAt();
        validatePeriod(start, end);
    }

    private boolean hasUpdateField(PortfolioDTO dto) {
        return dto.getCategory() != null
                || dto.getTitle() != null
                || dto.getSummary() != null
                || dto.getContent() != null
                || dto.getWorkStartAt() != null
                || dto.getWorkEndAt() != null
                || dto.getIsPublic() != null
                || dto.getBannerFileId() != null
                || dto.getFileIds() != null;
    }

    private void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new CustomException(ErrorCode.INVALID_PORTFOLIO_PERIOD);
        }
    }

    private PortfolioDTO findExistingPortfolio(int portfolioId) {
        if (portfolioId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        PortfolioDTO portfolio = portfolioMapper.findByIdIncludingDeleted(portfolioId);
        if (portfolio == null) {
            throw new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(portfolio.getIsDeleted())) {
            throw new CustomException(ErrorCode.DELETED_PORTFOLIO);
        }
        return portfolio;
    }

    private void validateReadable(PortfolioDTO portfolio, String currentEmail, String currentRole) {
        if (Objects.equals(portfolio.getEmail(), currentEmail)) {
            return;
        }
        if (ROLE_COMPANY.equals(currentRole) && Boolean.TRUE.equals(portfolio.getIsPublic())) {
            return;
        }
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private void validateOwner(PortfolioDTO portfolio, String email) {
        if (!Objects.equals(portfolio.getEmail(), email)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateAttachableFile(Integer fileId, String email, FileParentType targetType, Integer portfolioId) {
        if (fileId == null) {
            return;
        }
        if (fileId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        FileDTO file = fileService.findById(fileId);
        String ownerEmail = file.getUserEmail() != null ? file.getUserEmail() : file.getCompanyEmail();
        if (!Objects.equals(ownerEmail, email)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (!FileParentType.TEMP.equals(file.getParentType()) && !targetType.equals(file.getParentType())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (file.getParentId() != null && !Objects.equals(file.getParentId(), portfolioId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void attachBanner(Integer bannerFileId, String email, int portfolioId) {
        fileService.attachToParent(bannerFileId, FileParentType.PORTFOLIO_BANNER, portfolioId, email);
    }

    private void attachResultFiles(List<Integer> fileIds, String email, int portfolioId) {
        for (Integer fileId : fileIds) {
            fileService.attachToParent(fileId, FileParentType.PORTFOLIO_FILE, portfolioId, email);
        }
    }

    private PortfolioDTO hydrateCard(PortfolioDTO dto, String currentEmail) {
        if (dto == null) {
            return null;
        }
        dto.setOwner(Objects.equals(dto.getEmail(), currentEmail));
        if (dto.getBannerFileId() != null) {
            dto.setBannerFile(fileService.findById(dto.getBannerFileId()));
        }
        return dto;
    }

    private PortfolioDTO hydrateDetail(PortfolioDTO dto, String currentEmail) {
        hydrateCard(dto, currentEmail);
        if (dto != null) {
            dto.setFiles(fileService.findByParent(FileParentType.PORTFOLIO_FILE, dto.getPortfolioId()));
        }
        return dto;
    }

    private List<Integer> normalizeFileIds(List<Integer> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }

        Set<Integer> uniqueIds = new LinkedHashSet<>();
        for (Integer fileId : fileIds) {
            if (fileId == null || fileId <= 0) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            uniqueIds.add(fileId);
        }
        return new ArrayList<>(uniqueIds);
    }

    private SearchParams normalizeSearch(PortfolioSearchCondition condition) {
        PortfolioSearchCondition safeCondition = condition == null ? new PortfolioSearchCondition() : condition;
        String keyword = isBlank(safeCondition.getKeyword()) ? null : safeCondition.getKeyword().trim();
        String category = isBlank(safeCondition.getCategory())
                ? null
                : PortfolioCategory.normalize(safeCondition.getCategory());
        Boolean visibility = parseVisibility(safeCondition.getVisibility());

        return new SearchParams(
                keyword,
                visibility,
                category,
                safeCondition.getSafeSort(),
                safeCondition.getOffset(),
                safeCondition.getSafeSize(),
                safeCondition.toPageRequest()
        );
    }

    private Boolean parseVisibility(String visibility) {
        if (isBlank(visibility) || "ALL".equalsIgnoreCase(visibility)) {
            return null;
        }

        return switch (visibility.trim().toUpperCase()) {
            case "PUBLIC", "TRUE", "1" -> true;
            case "PRIVATE", "FALSE", "0" -> false;
            default -> throw new CustomException(ErrorCode.INVALID_INPUT);
        };
    }

    private void requireFreelancer(String role) {
        if (!ROLE_USER.equals(role)) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record SearchParams(String keyword,
                                Boolean visibility,
                                String category,
                                String sort,
                                int offset,
                                int size,
                                com.ssafy.lancit.common.page.dto.PageRequest pageRequest) {
    }
}
