package com.ssafy.lancit.domain.recruitment.application.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDetailResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationPortfolioSummaryResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationProfileSnapshotDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationProfileSnapshotRequest;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationRequest;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationStatusUpdateRequest;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationPortfolioSnapshotMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationProfileSnapshotMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.PortfolioPermissionMapper;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_COMPANY = "COMPANY";
    private static final int MAX_INTRO_LENGTH = 1000;
    private static final int MAX_PROFILE_DISPLAY_NAME_LENGTH = 100;
    private static final int MAX_PROFILE_INTRO_LENGTH = 30;
    private static final int MAX_PROFILE_DESCRIPTION_LENGTH = 200;
    private static final int MAX_PROFILE_TECH_STACK_LENGTH = 100;

    private final ApplicationMapper applicationMapper;
    private final PortfolioPermissionMapper portfolioPermissionMapper;
    private final PortfolioMapper portfolioMapper;
    private final PortfolioService portfolioService;
    private final ApplicationProfileSnapshotMapper applicationProfileSnapshotMapper;
    private final ApplicationPortfolioSnapshotMapper applicationPortfolioSnapshotMapper;
    private final FileService fileService;
    private final RecruitmentMapper recruitmentMapper;
    private final ContractMapper contractMapper;

    public PageResponse<ApplicationDetailResponse> getCompanyApplications(int recruitmentId,
                                                                          String companyEmail,
                                                                          String role,
                                                                          PageRequest pageRequest) {
        requireCompany(role);
        verifyRecruitmentOwner(recruitmentId, companyEmail);

        List<ApplicationDTO> applications = applicationMapper.findCompanyList(recruitmentId, pageRequest);
        long total = applicationMapper.countCompanyList(recruitmentId);
        List<ApplicationDetailResponse> content = applications.stream()
                .map(application -> toDetailResponse(application, false))
                .toList();
        return PageResponse.of(content, total, pageRequest);
    }

    @Transactional
    public ApplicationDetailResponse getCompanyApplication(int recruitmentId,
                                                           int applicationId,
                                                           String companyEmail,
                                                           String role) {
        requireCompany(role);
        verifyRecruitmentOwner(recruitmentId, companyEmail);

        ApplicationDTO application = findCompanyApplication(recruitmentId, applicationId);
        if (application.getViewedAt() == null) {
            applicationMapper.markViewedIfAbsent(applicationId);
            application = findCompanyApplication(recruitmentId, applicationId);
        }
        return toDetailResponse(application, true);
    }

    @Transactional
    public ApplicationDetailResponse apply(int recruitmentId,
                                           ApplicationRequest request,
                                           String freelancerEmail,
                                           String role) {
        requireUser(role);
        RecruitmentDTO recruitment = findOpenRecruitment(recruitmentId);
        ApplicationDTO existing =
                applicationMapper.findByRecruitmentAndApplicant(recruitmentId, freelancerEmail);
        if (existing != null && !ApplicationStatus.CANCELLED.equals(existing.getStatus())) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        List<Integer> portfolioIds = validatePortfolioIds(request, freelancerEmail);
        String intro = normalizeIntro(request.getIntro());
        if (existing != null) {
            return reactivateCancelled(
                    existing,
                    portfolioIds,
                    intro,
                    request.getPortfolioProfile(),
                    freelancerEmail,
                    role);
        }

        ApplicationDTO application = ApplicationDTO.builder()
                .recruitmentId(recruitment.getRecruitmentId())
                .recruitmentTitle(recruitment.getTitle())
                .applicantEmail(freelancerEmail)
                .intro(intro)
                .status(ApplicationStatus.PENDING)
                .build();

        try {
            applicationMapper.insert(application);
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }
        portfolioPermissionMapper.insertAll(application.getApplicationId(), portfolioIds);
        createPortfolioSnapshots(application.getApplicationId(), portfolioIds);
        createProfileSnapshot(application.getApplicationId(), freelancerEmail, request.getPortfolioProfile());
        return getMine(recruitmentId, freelancerEmail, role);
    }

    private ApplicationDetailResponse reactivateCancelled(ApplicationDTO application,
                                                          List<Integer> portfolioIds,
                                                          String intro,
                                                          ApplicationProfileSnapshotRequest profileRequest,
                                                          String freelancerEmail,
                                                          String role) {
        int applicationId = application.getApplicationId();
        List<Integer> previousSnapshotFileIds =
                applicationPortfolioSnapshotMapper.findFileIdsByApplicationId(applicationId);
        Integer previousProfileFileId = findProfileSnapshotFileId(applicationId);
        if (applicationMapper.reactivateCancelled(applicationId, intro) == 0) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        portfolioPermissionMapper.deleteByApplicationId(applicationId);
        applicationPortfolioSnapshotMapper.deleteByApplicationId(applicationId);
        applicationProfileSnapshotMapper.deleteTechStacksByApplicationId(applicationId);
        applicationProfileSnapshotMapper.deleteByApplicationId(applicationId);

        portfolioPermissionMapper.insertAll(applicationId, portfolioIds);
        createPortfolioSnapshots(applicationId, portfolioIds);
        createProfileSnapshot(applicationId, freelancerEmail, profileRequest);
        previousSnapshotFileIds.forEach(fileService::deletePortfolioFileIfUnreferenced);
        deleteProfileFileIfPresent(previousProfileFileId);
        return getMine(application.getRecruitmentId(), freelancerEmail, role);
    }

    public Map<String, Object> getCompanyApplicationPortfolio(int recruitmentId,
                                                              int applicationId,
                                                              int portfolioId,
                                                              String companyEmail,
                                                              String role) {
        requireCompany(role);
        verifyRecruitmentOwner(recruitmentId, companyEmail);
        findCompanyApplication(recruitmentId, applicationId);
        if (!portfolioPermissionMapper.existsCompanyPermission(
                applicationId, portfolioId, recruitmentId, companyEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        PortfolioDTO portfolio = applicationPortfolioSnapshotMapper.findPortfolio(applicationId, portfolioId);
        if (portfolio == null) {
            throw new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("portfolio", portfolio);
        result.put("files", applicationPortfolioSnapshotMapper.findFiles(applicationId, portfolioId));
        return result;
    }

    public String getCompanyApplicationPortfolioFileUrl(int recruitmentId,
                                                        int applicationId,
                                                        int portfolioId,
                                                        int fileId,
                                                        String companyEmail,
                                                        String role) {
        verifyCompanyPortfolioPermission(
                recruitmentId, applicationId, portfolioId, companyEmail, role);
        verifyApplicationPortfolioFile(applicationId, portfolioId, fileId);
        return fileService.getSignedUrl(fileId);
    }

    public String getCompanyApplicationPortfolioFileDownloadUrl(int recruitmentId,
                                                                int applicationId,
                                                                int portfolioId,
                                                                int fileId,
                                                                String companyEmail,
                                                                String role) {
        verifyCompanyPortfolioPermission(
                recruitmentId, applicationId, portfolioId, companyEmail, role);
        verifyApplicationPortfolioFile(applicationId, portfolioId, fileId);
        return fileService.getDownloadUrl(fileId);
    }

    public String getCompanyApplicationProfileImageUrl(int recruitmentId,
                                                       int applicationId,
                                                       String companyEmail,
                                                       String role) {
        requireCompany(role);
        verifyRecruitmentOwner(recruitmentId, companyEmail);
        findCompanyApplication(recruitmentId, applicationId);
        ApplicationProfileSnapshotDTO snapshot =
                applicationProfileSnapshotMapper.findByApplicationId(applicationId);
        if (snapshot == null || snapshot.getProfileFileId() == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return fileService.getSignedUrl(snapshot.getProfileFileId());
    }

    private void verifyCompanyPortfolioPermission(int recruitmentId,
                                                  int applicationId,
                                                  int portfolioId,
                                                  String companyEmail,
                                                  String role) {
        requireCompany(role);
        verifyRecruitmentOwner(recruitmentId, companyEmail);
        findCompanyApplication(recruitmentId, applicationId);
        if (!portfolioPermissionMapper.existsCompanyPermission(
                applicationId, portfolioId, recruitmentId, companyEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void verifyApplicationPortfolioFile(int applicationId, int portfolioId, int fileId) {
        if (!applicationPortfolioSnapshotMapper.existsFile(applicationId, portfolioId, fileId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    public ApplicationDetailResponse getMine(int recruitmentId, String freelancerEmail, String role) {
        requireUser(role);
        ApplicationDTO application = findMyApplication(recruitmentId, freelancerEmail);
        return toDetailResponse(application, true);
    }

    public PortfolioProfileDTO getMineProfile(int recruitmentId, String freelancerEmail, String role) {
        requireUser(role);
        ApplicationDTO application =
                applicationMapper.findByRecruitmentAndApplicant(recruitmentId, freelancerEmail);
        if (application != null && !ApplicationStatus.CANCELLED.equals(application.getStatus())) {
            PortfolioProfileDTO snapshot = findProfileSnapshot(application.getApplicationId());
            if (snapshot != null) {
                return snapshot;
            }
        }

        findOpenRecruitment(recruitmentId);
        return portfolioService.getMyProfile(freelancerEmail);
    }

    @Transactional
    public ApplicationDetailResponse updateMine(int recruitmentId,
                                                ApplicationRequest request,
                                                String freelancerEmail,
                                                String role) {
        requireUser(role);
        ApplicationDTO application = findMyApplication(recruitmentId, freelancerEmail);
        verifyMutable(application);
        findOpenRecruitment(recruitmentId);

        List<Integer> portfolioIds = validatePortfolioIds(request, freelancerEmail);
        int updated = applicationMapper.updateIntro(application.getApplicationId(), normalizeIntro(request.getIntro()));
        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS);
        }

        int applicationId = application.getApplicationId();
        List<Integer> previousSnapshotFileIds =
                applicationPortfolioSnapshotMapper.findFileIdsByApplicationId(applicationId);
        portfolioPermissionMapper.deleteByApplicationId(applicationId);
        portfolioPermissionMapper.insertAll(applicationId, portfolioIds);
        applicationPortfolioSnapshotMapper.deleteByApplicationId(applicationId);
        createPortfolioSnapshots(applicationId, portfolioIds);
        replaceProfileSnapshotIfRequested(applicationId, freelancerEmail, request.getPortfolioProfile());
        previousSnapshotFileIds.forEach(fileService::deletePortfolioFileIfUnreferenced);
        return getMine(recruitmentId, freelancerEmail, role);
    }

    @Transactional
    public void cancelMine(int recruitmentId, String freelancerEmail, String role) {
        requireUser(role);
        ApplicationDTO application = findMyApplication(recruitmentId, freelancerEmail);
        verifyCancelable(application);

        int updated = applicationMapper.cancel(application.getApplicationId());
        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS);
        }
        // 취소 후에도 회사가 과거 지원 이력을 확인할 수 있도록 포트폴리오 연결은 유지한다.
    }

    @Transactional
    public ApplicationDetailResponse updateStatus(int recruitmentId,
                                                  int applicationId,
                                                  ApplicationStatusUpdateRequest request,
                                                  String companyEmail,
                                                  String role) {
        requireCompany(role);
        ApplicationStatus targetStatus = parseTargetStatus(request);
        RecruitmentDTO recruitment = verifyRecruitmentOwner(recruitmentId, companyEmail);
        ApplicationDTO application = findCompanyApplication(recruitmentId, applicationId);
        if (!ApplicationStatus.PENDING.equals(application.getStatus())) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS_CHANGE);
        }
        if (ApplicationStatus.ACCEPTED.equals(targetStatus)
                && contractMapper.existsActiveContract(recruitmentId, application.getApplicantEmail())) {
            throw new CustomException(ErrorCode.CONTRACT_ALREADY_EXISTS);
        }

        if (ApplicationStatus.ACCEPTED.equals(targetStatus)
                && recruitmentMapper.closeIfOpen(recruitmentId) == 0) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_OPEN);
        }

        int updated = applicationMapper.updateStatusIfPending(applicationId, targetStatus);
        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS_CHANGE);
        }

        if (ApplicationStatus.ACCEPTED.equals(targetStatus)) {
            ContractDTO contract = ContractDTO.builder()
                    .recruitmentId(recruitmentId)
                    .companyEmail(recruitment.getCompanyEmail())
                    .freelancerEmail(application.getApplicantEmail())
                    .status(ContractStatus.WAITING)
                    .build();
            contractMapper.insert(contract);
            if (applicationMapper.attachContract(applicationId, contract.getContractId()) == 0) {
                throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS_CHANGE);
            }
        }

        return toDetailResponse(findCompanyApplication(recruitmentId, applicationId), false);
    }

    private void requireUser(String role) {
        if (!ROLE_USER.equals(role)) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
    }

    private void requireCompany(String role) {
        if (!ROLE_COMPANY.equals(role)) {
            throw new CustomException(ErrorCode.RECRUITMENT_COMPANY_ONLY);
        }
    }

    private RecruitmentDTO verifyRecruitmentOwner(int recruitmentId, String companyEmail) {
        RecruitmentDTO recruitment = recruitmentMapper.findById(recruitmentId);
        if (recruitment == null) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
        }
        if (!companyEmail.equals(recruitment.getCompanyEmail())) {
            throw new CustomException(ErrorCode.RECRUITMENT_FORBIDDEN);
        }
        return recruitment;
    }

    private RecruitmentDTO findOpenRecruitment(int recruitmentId) {
        RecruitmentDTO recruitment = recruitmentMapper.findById(recruitmentId);
        if (recruitment == null) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
        }
        if (!RecruitmentStatus.OPEN.equals(recruitment.getStatus())
                || (recruitment.getRecruitmentEndAt() != null
                && recruitment.getRecruitmentEndAt().isBefore(LocalDateTime.now()))) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_OPEN);
        }
        return recruitment;
    }

    private ApplicationDTO findMyApplication(int recruitmentId, String freelancerEmail) {
        ApplicationDTO application = applicationMapper.findByRecruitmentAndApplicant(recruitmentId, freelancerEmail);
        if (application == null) {
            throw new CustomException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private ApplicationDTO findCompanyApplication(int recruitmentId, int applicationId) {
        ApplicationDTO application = applicationMapper.findCompanyDetail(recruitmentId, applicationId);
        if (application == null) {
            throw new CustomException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private void verifyMutable(ApplicationDTO application) {
        if (!ApplicationStatus.PENDING.equals(application.getStatus())) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS);
        }
        if (application.getViewedAt() != null) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_VIEWED);
        }
    }

    private void verifyCancelable(ApplicationDTO application) {
        if (ApplicationStatus.CANCELLED.equals(application.getStatus())) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_CANCELLED);
        }
        if (!ApplicationStatus.PENDING.equals(application.getStatus())) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS);
        }
        if (application.getViewedAt() != null) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_VIEWED);
        }
    }

    private ApplicationStatus parseTargetStatus(ApplicationStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS_VALUE);
        }
        try {
            ApplicationStatus status =
                    ApplicationStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
            if (!ApplicationStatus.ACCEPTED.equals(status) && !ApplicationStatus.REJECTED.equals(status)) {
                throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS_VALUE);
            }
            return status;
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATUS_VALUE);
        }
    }

    private List<Integer> validatePortfolioIds(ApplicationRequest request, String freelancerEmail) {
        if (request == null || request.getPortfolioIds() == null || request.getPortfolioIds().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_PORTFOLIO);
        }

        LinkedHashSet<Integer> uniqueIds = new LinkedHashSet<>();
        for (Integer portfolioId : request.getPortfolioIds()) {
            if (portfolioId == null) {
                throw new CustomException(ErrorCode.INVALID_APPLICATION_PORTFOLIO);
            }
            uniqueIds.add(portfolioId);
        }

        List<Integer> portfolioIds = new ArrayList<>(uniqueIds);
        if (portfolioIds.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_PORTFOLIO);
        }

        int ownedCount = portfolioMapper.countOwnedActiveByIds(freelancerEmail, portfolioIds);
        if (ownedCount != portfolioIds.size()) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_PORTFOLIO);
        }
        return portfolioIds;
    }

    private String normalizeIntro(String intro) {
        if (intro == null) {
            return "";
        }
        String normalized = intro.trim();
        if (normalized.length() > MAX_INTRO_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private ApplicationDetailResponse toDetailResponse(ApplicationDTO application, boolean includePortfolioProfile) {
        List<ApplicationPortfolioSummaryResponse> portfolios =
                applicationPortfolioSnapshotMapper.findSummariesByApplicationId(application.getApplicationId());
        PortfolioProfileDTO portfolioProfile = includePortfolioProfile
                ? findProfileSnapshot(application.getApplicationId())
                : null;

        return ApplicationDetailResponse.builder()
                .applicationId(application.getApplicationId())
                .recruitmentId(application.getRecruitmentId())
                .contractId(application.getContractId())
                .recruitmentTitle(application.getRecruitmentTitle())
                .applicantEmail(application.getApplicantEmail())
                .applicantName(portfolioProfile == null
                        ? application.getApplicantName()
                        : portfolioProfile.getDisplayName())
                .intro(application.getIntro())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .canceledAt(application.getCanceledAt())
                .viewedAt(application.getViewedAt())
                .portfolioProfile(portfolioProfile)
                .portfolios(portfolios == null ? List.of() : portfolios)
                .build();
    }

    private void createPortfolioSnapshots(int applicationId, List<Integer> portfolioIds) {
        for (int index = 0; index < portfolioIds.size(); index++) {
            int portfolioId = portfolioIds.get(index);
            if (applicationPortfolioSnapshotMapper.insertPortfolio(applicationId, portfolioId, index) == 0) {
                throw new CustomException(ErrorCode.INVALID_APPLICATION_PORTFOLIO);
            }
            applicationPortfolioSnapshotMapper.insertFiles(applicationId, portfolioId);
        }
    }

    private void createProfileSnapshot(int applicationId,
                                       String freelancerEmail,
                                       ApplicationProfileSnapshotRequest profileRequest) {
        PortfolioProfileDTO profile = resolveProfileSnapshot(freelancerEmail, profileRequest);
        ApplicationProfileSnapshotDTO snapshot = ApplicationProfileSnapshotDTO.builder()
                .applicationId(applicationId)
                .displayName(profile.getDisplayName())
                .jobCategory(profile.getJobCategory())
                .profileFileId(profile.getProfileFileId())
                .intro(profile.getIntro())
                .description(profile.getDescription())
                .isPortfolioPublic(profile.getIsPortfolioPublic())
                .sourceProfileUpdatedAt(profile.getUpdatedAt())
                .build();
        applicationProfileSnapshotMapper.insert(snapshot);

        List<String> techStacks = profile.getTechStacks();
        for (int index = 0; index < techStacks.size(); index++) {
            applicationProfileSnapshotMapper.insertTechStack(applicationId, techStacks.get(index), index);
        }
    }

    private PortfolioProfileDTO resolveProfileSnapshot(String freelancerEmail,
                                                       ApplicationProfileSnapshotRequest profileRequest) {
        if (profileRequest == null) {
            return portfolioService.getMyProfile(freelancerEmail);
        }

        prepareProfileSnapshotFile(profileRequest.getProfileFileId(), freelancerEmail);
        return PortfolioProfileDTO.builder()
                .freelancerEmail(freelancerEmail)
                .displayName(normalizeProfileDisplayName(profileRequest.getDisplayName()))
                .jobCategory(requireProfileJobCategory(profileRequest.getJobCategory()))
                .profileFileId(profileRequest.getProfileFileId())
                .intro(normalizeProfileIntro(profileRequest.getIntro()))
                .description(normalizeProfileDescription(profileRequest.getDescription()))
                .isPortfolioPublic(Boolean.TRUE.equals(profileRequest.getIsPortfolioPublic()))
                .techStacks(normalizeProfileTechStacks(profileRequest.getTechStacks()))
                .build();
    }

    private void replaceProfileSnapshotIfRequested(int applicationId,
                                                   String freelancerEmail,
                                                   ApplicationProfileSnapshotRequest profileRequest) {
        if (profileRequest == null) {
            return;
        }

        Integer previousProfileFileId = findProfileSnapshotFileId(applicationId);
        applicationProfileSnapshotMapper.deleteTechStacksByApplicationId(applicationId);
        applicationProfileSnapshotMapper.deleteByApplicationId(applicationId);
        createProfileSnapshot(applicationId, freelancerEmail, profileRequest);
        deleteProfileFileIfPresent(previousProfileFileId);
    }

    private Integer findProfileSnapshotFileId(int applicationId) {
        ApplicationProfileSnapshotDTO snapshot =
                applicationProfileSnapshotMapper.findByApplicationId(applicationId);
        return snapshot == null ? null : snapshot.getProfileFileId();
    }

    private void deleteProfileFileIfPresent(Integer profileFileId) {
        if (profileFileId != null) {
            fileService.deleteProfileIfUnreferenced(profileFileId);
        }
    }

    private void prepareProfileSnapshotFile(Integer profileFileId, String freelancerEmail) {
        if (profileFileId == null) {
            return;
        }

        FileDTO file = fileService.findById(profileFileId);
        String fileOwnerEmail = file.getUserEmail() != null ? file.getUserEmail() : file.getCompanyEmail();
        if (!freelancerEmail.equals(fileOwnerEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        FileParentType parentType = file.getParentType();
        if (FileParentType.TEMP.equals(parentType) || FileParentType.TEMP_SIGNATURE.equals(parentType)) {
            fileService.promoteOwned(profileFileId, FileParentType.APPLICATION_PROFILE, freelancerEmail);
            return;
        }
        if (!FileParentType.PROFILE.equals(parentType)
                && !FileParentType.PORTFOLIO_PROFILE.equals(parentType)
                && !FileParentType.APPLICATION_PROFILE.equals(parentType)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private JobCategory requireProfileJobCategory(JobCategory jobCategory) {
        if (jobCategory == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return jobCategory;
    }

    private String normalizeProfileDisplayName(String displayName) {
        if (!hasText(displayName)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String normalized = displayName.trim();
        if (normalized.length() > MAX_PROFILE_DISPLAY_NAME_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private String normalizeProfileIntro(String intro) {
        if (!hasText(intro)) {
            return "";
        }
        String normalized = intro.trim();
        if (normalized.length() > MAX_PROFILE_INTRO_LENGTH) {
            throw new CustomException(ErrorCode.PORTFOLIO_PROFILE_INTRO_TOO_LONG);
        }
        return normalized;
    }

    private String normalizeProfileDescription(String description) {
        if (!hasText(description)) {
            return "";
        }
        String normalized = description.trim();
        if (normalized.length() > MAX_PROFILE_DESCRIPTION_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private List<String> normalizeProfileTechStacks(List<String> techStacks) {
        if (techStacks == null || techStacks.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String techStack : techStacks) {
            if (!hasText(techStack)) {
                continue;
            }
            String trimmed = techStack.trim();
            if (trimmed.length() > MAX_PROFILE_TECH_STACK_LENGTH) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            normalized.add(trimmed);
        }
        return List.copyOf(normalized);
    }

    private PortfolioProfileDTO findProfileSnapshot(int applicationId) {
        ApplicationProfileSnapshotDTO snapshot =
                applicationProfileSnapshotMapper.findByApplicationId(applicationId);
        if (snapshot == null) {
            return null;
        }
        return PortfolioProfileDTO.builder()
                .displayName(snapshot.getDisplayName())
                .jobCategory(snapshot.getJobCategory())
                .profileFileId(snapshot.getProfileFileId())
                .intro(snapshot.getIntro())
                .description(snapshot.getDescription())
                .isPortfolioPublic(snapshot.getIsPortfolioPublic())
                .techStacks(applicationProfileSnapshotMapper.findTechStacksByApplicationId(applicationId))
                .updatedAt(snapshot.getSourceProfileUpdatedAt())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
