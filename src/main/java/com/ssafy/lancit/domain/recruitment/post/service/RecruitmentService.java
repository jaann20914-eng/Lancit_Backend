package com.ssafy.lancit.domain.recruitment.post.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.freelancer.mapper.FreelancerBookmarkMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentCreateRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDetailResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentListItemResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentPermissionResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentSearchCondition;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentStatusUpdateRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentTechStackDTO;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentUpdateRequest;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.RecruitmentSortType;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import com.ssafy.lancit.global.enums.RecruitmentViewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private static final String ROLE_COMPANY = "COMPANY";
    private static final String ROLE_USER = "USER";
    private static final String TAB_ALL = "ALL";
    private static final String TAB_APPLIED = "APPLIED";
    private static final String TAB_BOOKMARKED = "BOOKMARKED";
    private static final Set<String> LIST_TABS = Set.of(TAB_ALL, TAB_APPLIED, TAB_BOOKMARKED);
    private static final Set<RecruitmentStatus> CHANGEABLE_STATUSES =
            Set.of(RecruitmentStatus.OPEN, RecruitmentStatus.CLOSED, RecruitmentStatus.CANCELLED);

    private final RecruitmentMapper recruitmentMapper;
    private final FreelancerBookmarkMapper freelancerBookmarkMapper;
    private final FileService fileService;

    public PageResponse<RecruitmentListItemResponse> getList(RecruitmentSearchCondition condition,
                                                              PageRequest pageRequest,
                                                              String viewerEmail,
                                                              String viewerRole) {
        RecruitmentSearchCondition normalized = normalizeCondition(condition, true);
        normalized.setCurrentEmail(ROLE_USER.equals(viewerRole) ? viewerEmail : null);
        validateListTabAccess(normalized, viewerEmail, viewerRole);
        List<RecruitmentDTO> list = recruitmentMapper.findList(normalized, pageRequest);
        long total = recruitmentMapper.countList(normalized);
        Map<Integer, List<String>> techStacks = findTechStacks(list);
        Set<Integer> appliedRecruitmentIds = findAppliedRecruitmentIds(list, viewerEmail, viewerRole);
        Set<Integer> bookmarkedRecruitmentIds = findBookmarkedRecruitmentIds(list, viewerEmail, viewerRole);
        List<RecruitmentListItemResponse> content = list.stream()
                .map(dto -> toListResponse(
                        dto,
                        techStacks.get(dto.getRecruitmentId()),
                        viewerEmail,
                        viewerRole,
                        bookmarkedRecruitmentIds.contains(dto.getRecruitmentId()),
                        appliedRecruitmentIds.contains(dto.getRecruitmentId())))
                .toList();
        return PageResponse.of(content, total, pageRequest);
    }

    public PageResponse<RecruitmentListItemResponse> getMyList(String companyEmail,
                                                                String role,
                                                                RecruitmentSearchCondition condition,
                                                                PageRequest pageRequest) {
        requireCompany(role);
        RecruitmentSearchCondition normalized = condition == null ? new RecruitmentSearchCondition() : condition;
        normalized.setTab(null);
        normalized = normalizeCondition(normalized, false);
        normalized.setCurrentEmail(null);
        List<RecruitmentDTO> list = recruitmentMapper.findMyList(companyEmail, normalized, pageRequest);
        long total = recruitmentMapper.countMyList(companyEmail, normalized);
        Map<Integer, List<String>> techStacks = findTechStacks(list);
        List<RecruitmentListItemResponse> content = list.stream()
                .map(dto -> toListResponse(dto, techStacks.get(dto.getRecruitmentId()), companyEmail, role, false, false))
                .toList();
        return PageResponse.of(content, total, pageRequest);
    }

    public PageResponse<RecruitmentListItemResponse> getBookmarkedList(String freelancerEmail,
                                                                       String role,
                                                                       RecruitmentSearchCondition condition,
                                                                       PageRequest pageRequest) {
        requireUser(role);
        RecruitmentSearchCondition bookmarkedCondition =
                condition == null ? new RecruitmentSearchCondition() : condition;
        bookmarkedCondition.setTab(TAB_BOOKMARKED);
        return getList(bookmarkedCondition, pageRequest, freelancerEmail, role);
    }

    public RecruitmentDetailResponse getOne(int recruitmentId, String viewerEmail, String viewerRole) {
        RecruitmentDTO dto = findExisting(recruitmentId);
        List<String> techStacks = recruitmentMapper.findTechStacksByRecruitmentId(recruitmentId);
        return toDetailResponse(
                dto,
                techStacks,
                viewerEmail,
                viewerRole,
                isBookmarked(dto, viewerEmail, viewerRole),
                isApplied(dto, viewerEmail, viewerRole));
    }

    @Transactional
    public RecruitmentDetailResponse create(RecruitmentCreateRequest request, String companyEmail, String role) {
        requireCompany(role);
        RecruitmentDTO dto = toDto(request, companyEmail);
        validateForSave(dto);

        recruitmentMapper.insertRecruitment(dto);
        List<String> techStacks = normalizeTechStacks(request.getTechStacks());
        if (!techStacks.isEmpty()) {
            recruitmentMapper.insertTechStacks(dto.getRecruitmentId(), techStacks);
        }
        attachImage(dto.getImageFileId(), dto.getRecruitmentId(), companyEmail);
        return getOne(dto.getRecruitmentId(), companyEmail, role);
    }

    @Transactional
    public RecruitmentDetailResponse update(int recruitmentId,
                                            RecruitmentUpdateRequest request,
                                            String companyEmail,
                                            String role) {
        requireCompany(role);
        RecruitmentDTO existing = findExisting(recruitmentId);
        verifyOwner(existing, companyEmail);
        verifyNoActiveApplications(recruitmentId);

        RecruitmentDTO dto = toDto(request, existing.getCompanyEmail(), existing.getStatus());
        validateForSave(dto);
        int updated = recruitmentMapper.updateRecruitment(recruitmentId, dto);
        if (updated == 0) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
        }

        recruitmentMapper.deleteTechStacks(recruitmentId);
        List<String> techStacks = normalizeTechStacks(request.getTechStacks());
        if (!techStacks.isEmpty()) {
            recruitmentMapper.insertTechStacks(recruitmentId, techStacks);
        }

        syncImage(existing.getImageFileId(), dto.getImageFileId(), recruitmentId, companyEmail);
        return getOne(recruitmentId, companyEmail, role);
    }

    @Transactional
    public void delete(int recruitmentId, String companyEmail, String role) {
        requireCompany(role);
        RecruitmentDTO existing = findExisting(recruitmentId);
        verifyOwner(existing, companyEmail);
        verifyNoActiveApplications(recruitmentId);

        int deleted = recruitmentMapper.softDeleteRecruitment(recruitmentId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
        }
        if (existing.getImageFileId() != null) {
            fileService.deleteRecruitmentImageIfUnreferenced(existing.getImageFileId());
        }
    }

    @Transactional
    public RecruitmentDetailResponse updateStatus(int recruitmentId,
                                                  RecruitmentStatusUpdateRequest request,
                                                  String companyEmail,
                                                  String role) {
        requireCompany(role);
        RecruitmentStatus status = parseChangeableStatus(request);
        RecruitmentDTO existing = findExisting(recruitmentId);
        verifyOwner(existing, companyEmail);

        int updated = recruitmentMapper.updateStatus(recruitmentId, status);
        if (updated == 0) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
        }
        return getOne(recruitmentId, companyEmail, role);
    }

    public RecruitmentCreateRequest getCopySource(int recruitmentId, String companyEmail, String role) {
        requireCompany(role);
        RecruitmentDTO existing = findExisting(recruitmentId);
        verifyOwner(existing, companyEmail);

        RecruitmentCreateRequest response = new RecruitmentCreateRequest();
        response.setTitle(existing.getTitle());
        response.setSummary(existing.getSummary());
        response.setContent(existing.getContent());
        response.setRequirements(existing.getRequirements());
        response.setJobCategory(existing.getJobCategory());
        response.setRecruitmentCategory(existing.getRecruitmentCategory());
        response.setStatus(RecruitmentStatus.OPEN);
        response.setWorkLocation(existing.getWorkLocation());
        response.setBudget(existing.getBudget());
        // 재등록 공고가 기존 공고의 파일 row를 공유하지 않도록 이미지는 복사 대상에서 제외한다.
        response.setImageFileId(null);
        response.setContractStartAt(existing.getContractStartAt());
        response.setContractEndAt(existing.getContractEndAt());
        response.setRecruitmentStartAt(null);
        response.setRecruitmentEndAt(null);
        response.setTechStacks(recruitmentMapper.findTechStacksByRecruitmentId(recruitmentId));
        return response;
    }

    private RecruitmentDTO findExisting(int recruitmentId) {
        RecruitmentDTO dto = recruitmentMapper.findById(recruitmentId);
        if (dto == null) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
        }
        return dto;
    }

    private void requireCompany(String role) {
        if (!ROLE_COMPANY.equals(role)) {
            throw new CustomException(ErrorCode.RECRUITMENT_COMPANY_ONLY);
        }
    }

    private void requireUser(String role) {
        if (!ROLE_USER.equals(role)) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
    }

    private void verifyOwner(RecruitmentDTO dto, String companyEmail) {
        if (dto == null || !companyEmail.equals(dto.getCompanyEmail())) {
            throw new CustomException(ErrorCode.RECRUITMENT_FORBIDDEN);
        }
    }

    private void verifyNoActiveApplications(int recruitmentId) {
        if (recruitmentMapper.countActiveApplications(recruitmentId) > 0) {
            throw new CustomException(ErrorCode.RECRUITMENT_HAS_ACTIVE_APPLICATIONS);
        }
    }

    private RecruitmentStatus parseChangeableStatus(RecruitmentStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_STATUS);
        }

        try {
            RecruitmentStatus status = RecruitmentStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
            if (!CHANGEABLE_STATUSES.contains(status)) {
                throw new CustomException(ErrorCode.INVALID_RECRUITMENT_STATUS);
            }
            return status;
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_STATUS);
        }
    }

    private RecruitmentDTO toDto(RecruitmentCreateRequest request, String companyEmail) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return RecruitmentDTO.builder()
                .companyEmail(companyEmail)
                .title(trim(request.getTitle()))
                .summary(trim(request.getSummary()))
                .content(trim(request.getContent()))
                .requirements(trimToNull(request.getRequirements()))
                .jobCategory(request.getJobCategory())
                .recruitmentCategory(request.getRecruitmentCategory())
                .status(request.getStatus() == null ? RecruitmentStatus.OPEN : request.getStatus())
                .workLocation(trimToNull(request.getWorkLocation()))
                .budget(request.getBudget() == null ? 0 : request.getBudget())
                .imageFileId(request.getImageFileId())
                .contractStartAt(request.getContractStartAt())
                .contractEndAt(request.getContractEndAt())
                .recruitmentStartAt(request.getRecruitmentStartAt() == null
                        ? LocalDateTime.now()
                        : request.getRecruitmentStartAt())
                .recruitmentEndAt(request.getRecruitmentEndAt())
                .build();
    }

    private RecruitmentDTO toDto(RecruitmentUpdateRequest request,
                                 String companyEmail,
                                 RecruitmentStatus status) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return RecruitmentDTO.builder()
                .companyEmail(companyEmail)
                .title(trim(request.getTitle()))
                .summary(trim(request.getSummary()))
                .content(trim(request.getContent()))
                .requirements(trimToNull(request.getRequirements()))
                .jobCategory(request.getJobCategory())
                .recruitmentCategory(request.getRecruitmentCategory())
                .status(status)
                .workLocation(trimToNull(request.getWorkLocation()))
                .budget(request.getBudget() == null ? 0 : request.getBudget())
                .imageFileId(request.getImageFileId())
                .contractStartAt(request.getContractStartAt())
                .contractEndAt(request.getContractEndAt())
                .recruitmentStartAt(request.getRecruitmentStartAt())
                .recruitmentEndAt(request.getRecruitmentEndAt())
                .build();
    }

    private void validateForSave(RecruitmentDTO dto) {
        if (!hasText(dto.getTitle()) || !hasText(dto.getSummary()) || !hasText(dto.getContent())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getJobCategory() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getRecruitmentCategory() == null) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_CATEGORY);
        }
        if (dto.getStatus() == null) {
            dto.setStatus(RecruitmentStatus.OPEN);
        }
        if (dto.getBudget() < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getRecruitmentStartAt() == null) {
            dto.setRecruitmentStartAt(LocalDateTime.now());
        }
        if (dto.getRecruitmentEndAt() != null
                && dto.getRecruitmentStartAt().isAfter(dto.getRecruitmentEndAt())) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_PERIOD);
        }
        if (dto.getContractStartAt() != null && dto.getContractEndAt() != null
                && dto.getContractStartAt().isAfter(dto.getContractEndAt())) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_PERIOD);
        }
    }

    private RecruitmentSearchCondition normalizeCondition(RecruitmentSearchCondition condition, boolean defaultTab) {
        RecruitmentSearchCondition normalized = condition == null ? new RecruitmentSearchCondition() : condition;
        normalized.setKeyword(trimToNull(normalized.getKeyword()));
        if (normalized.getSort() == null) {
            normalized.setSort(RecruitmentSortType.LATEST);
        }
        normalizeTab(normalized, defaultTab);

        String status = trimToNull(normalized.getStatus());
        if (status == null) {
            normalized.setStatus(null);
            return normalized;
        }

        String upperStatus = status.toUpperCase(Locale.ROOT);
        Set<String> allowed = Set.of("OPEN", "CLOSED", "CANCELLED", "EXPIRED");
        if (!allowed.contains(upperStatus)) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_STATUS);
        }
        normalized.setStatus(upperStatus);
        return normalized;
    }

    private void normalizeTab(RecruitmentSearchCondition condition, boolean defaultTab) {
        String tab = trimToNull(condition.getTab());
        if (tab == null) {
            condition.setTab(defaultTab ? TAB_ALL : null);
            return;
        }

        String upperTab = tab.toUpperCase(Locale.ROOT);
        if (!LIST_TABS.contains(upperTab)) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_TAB);
        }
        condition.setTab(upperTab);
    }

    private void validateListTabAccess(RecruitmentSearchCondition condition, String viewerEmail, String viewerRole) {
        String tab = condition.getSafeTab();
        if (!TAB_APPLIED.equals(tab) && !TAB_BOOKMARKED.equals(tab)) {
            return;
        }
        if (viewerEmail == null || viewerRole == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        requireUser(viewerRole);
    }

    private Map<Integer, List<String>> findTechStacks(List<RecruitmentDTO> recruitments) {
        List<Integer> recruitmentIds = recruitments.stream()
                .map(RecruitmentDTO::getRecruitmentId)
                .toList();
        if (recruitmentIds.isEmpty()) {
            return Map.of();
        }
        return recruitmentMapper.findTechStacksByRecruitmentIds(recruitmentIds).stream()
                .collect(Collectors.groupingBy(
                        RecruitmentTechStackDTO::getRecruitmentId,
                        Collectors.mapping(RecruitmentTechStackDTO::getTagName, Collectors.toList())));
    }

    private Set<Integer> findBookmarkedRecruitmentIds(List<RecruitmentDTO> recruitments,
                                                      String viewerEmail,
                                                      String viewerRole) {
        if (!ROLE_USER.equals(viewerRole) || viewerEmail == null || recruitments.isEmpty()) {
            return Set.of();
        }

        List<Integer> recruitmentIds = recruitments.stream()
                .map(RecruitmentDTO::getRecruitmentId)
                .toList();
        List<Integer> bookmarkedRecruitmentIds =
                freelancerBookmarkMapper.findBookmarkedRecruitmentIds(viewerEmail, recruitmentIds);
        if (bookmarkedRecruitmentIds == null || bookmarkedRecruitmentIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(bookmarkedRecruitmentIds);
    }

    private Set<Integer> findAppliedRecruitmentIds(List<RecruitmentDTO> recruitments,
                                                   String viewerEmail,
                                                   String viewerRole) {
        if (!ROLE_USER.equals(viewerRole) || viewerEmail == null || recruitments.isEmpty()) {
            return Set.of();
        }

        List<Integer> recruitmentIds = recruitments.stream()
                .map(RecruitmentDTO::getRecruitmentId)
                .toList();
        List<Integer> appliedRecruitmentIds =
                recruitmentMapper.findAppliedRecruitmentIds(viewerEmail, recruitmentIds);
        if (appliedRecruitmentIds == null || appliedRecruitmentIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(appliedRecruitmentIds);
    }

    private boolean isBookmarked(RecruitmentDTO dto, String viewerEmail, String viewerRole) {
        return ROLE_USER.equals(viewerRole)
                && viewerEmail != null
                && freelancerBookmarkMapper.exists(viewerEmail, dto.getRecruitmentId());
    }

    private boolean isApplied(RecruitmentDTO dto, String viewerEmail, String viewerRole) {
        return findAppliedRecruitmentIds(List.of(dto), viewerEmail, viewerRole)
                .contains(dto.getRecruitmentId());
    }

    private List<String> normalizeTechStacks(List<String> techStacks) {
        if (techStacks == null || techStacks.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : techStacks) {
            String value = trimToNull(tag);
            if (value != null) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }

    private void attachImage(Integer imageFileId, int recruitmentId, String companyEmail) {
        if (imageFileId != null) {
            fileService.attachToParent(imageFileId, FileParentType.RECRUITMENT_IMAGE, recruitmentId, companyEmail);
        }
    }

    private void syncImage(Integer oldImageFileId, Integer newImageFileId, int recruitmentId, String companyEmail) {
        if (oldImageFileId != null && oldImageFileId.equals(newImageFileId)) {
            return;
        }
        attachImage(newImageFileId, recruitmentId, companyEmail);
        if (oldImageFileId != null) {
            fileService.deleteRecruitmentImageIfUnreferenced(oldImageFileId);
        }
    }

    private RecruitmentListItemResponse toListResponse(RecruitmentDTO dto,
                                                       List<String> techStacks,
                                                       String viewerEmail,
                                                       String viewerRole,
                                                       boolean isBookmarked,
                                                       boolean isApplied) {
        RecruitmentPermissionResponse permission = buildPermission(dto, viewerEmail, viewerRole, isBookmarked, isApplied);
        return RecruitmentListItemResponse.builder()
                .recruitmentId(dto.getRecruitmentId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .companyEmail(dto.getCompanyEmail())
                .companyName(dto.getCompanyName())
                .imageFileId(dto.getImageFileId())
                .jobCategory(dto.getJobCategory())
                .recruitmentCategory(dto.getRecruitmentCategory())
                .techStacks(techStacks == null ? List.of() : techStacks)
                .workLocation(dto.getWorkLocation())
                .budget(dto.getBudget())
                .status(dto.getStatus())
                .viewStatus(resolveViewStatus(dto))
                .recruitmentStartAt(dto.getRecruitmentStartAt())
                .recruitmentEndAt(dto.getRecruitmentEndAt())
                .contractStartAt(dto.getContractStartAt())
                .contractEndAt(dto.getContractEndAt())
                .createdAt(dto.getCreatedAt())
                .applicantCount(dto.getApplicantCount())
                .isMine(permission.getIsMine())
                .canEdit(permission.getCanEdit())
                .canDelete(permission.getCanDelete())
                .canChangeStatus(permission.getCanChangeStatus())
                .canApply(permission.getCanApply())
                .isApplied(permission.getIsApplied())
                .isBookmarked(permission.getIsBookmarked())
                .build();
    }

    private RecruitmentDetailResponse toDetailResponse(RecruitmentDTO dto,
                                                       List<String> techStacks,
                                                       String viewerEmail,
                                                       String viewerRole,
                                                       boolean isBookmarked,
                                                       boolean isApplied) {
        RecruitmentPermissionResponse permission = buildPermission(dto, viewerEmail, viewerRole, isBookmarked, isApplied);
        return RecruitmentDetailResponse.builder()
                .recruitmentId(dto.getRecruitmentId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .requirements(dto.getRequirements())
                .companyEmail(dto.getCompanyEmail())
                .companyName(dto.getCompanyName())
                .imageFileId(dto.getImageFileId())
                .jobCategory(dto.getJobCategory())
                .recruitmentCategory(dto.getRecruitmentCategory())
                .techStacks(techStacks == null ? List.of() : techStacks)
                .workLocation(dto.getWorkLocation())
                .budget(dto.getBudget())
                .status(dto.getStatus())
                .viewStatus(resolveViewStatus(dto))
                .recruitmentStartAt(dto.getRecruitmentStartAt())
                .recruitmentEndAt(dto.getRecruitmentEndAt())
                .contractStartAt(dto.getContractStartAt())
                .contractEndAt(dto.getContractEndAt())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .applicantCount(dto.getApplicantCount())
                .isMine(permission.getIsMine())
                .canEdit(permission.getCanEdit())
                .canDelete(permission.getCanDelete())
                .canChangeStatus(permission.getCanChangeStatus())
                .canApply(permission.getCanApply())
                .isApplied(permission.getIsApplied())
                .isBookmarked(permission.getIsBookmarked())
                .permission(permission)
                .build();
    }

    private RecruitmentPermissionResponse buildPermission(RecruitmentDTO dto,
                                                          String viewerEmail,
                                                          String viewerRole,
                                                          boolean isBookmarked,
                                                          boolean isApplied) {
        boolean isMine = ROLE_COMPANY.equals(viewerRole)
                && viewerEmail != null
                && viewerEmail.equals(dto.getCompanyEmail());
        boolean hasActiveApplications = dto.getApplicantCount() > 0;
        RecruitmentViewStatus viewStatus = resolveViewStatus(dto);
        boolean canApply = ROLE_USER.equals(viewerRole)
                && RecruitmentViewStatus.OPEN.equals(viewStatus)
                && !isApplied;

        return RecruitmentPermissionResponse.builder()
                .isMine(isMine)
                .canEdit(isMine && !hasActiveApplications)
                .canDelete(isMine && !hasActiveApplications)
                .canChangeStatus(isMine)
                .canApply(canApply)
                .isApplied(isApplied)
                .isBookmarked(isBookmarked)
                .build();
    }

    private RecruitmentViewStatus resolveViewStatus(RecruitmentDTO dto) {
        if (RecruitmentStatus.OPEN.equals(dto.getStatus())
                && dto.getRecruitmentEndAt() != null
                && dto.getRecruitmentEndAt().isBefore(LocalDateTime.now())) {
            return RecruitmentViewStatus.EXPIRED;
        }
        return RecruitmentViewStatus.valueOf(dto.getStatus().name());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return hasText(trimmed) ? trimmed : null;
    }
}
