package com.ssafy.lancit.domain.recruitment.application.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioProfileMapper;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDetailResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationPortfolioSummaryResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationRequest;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationStatusUpdateRequest;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.PortfolioPermissionMapper;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_COMPANY = "COMPANY";
    private static final int MAX_INTRO_LENGTH = 1000;

    private final ApplicationMapper applicationMapper;
    private final PortfolioPermissionMapper portfolioPermissionMapper;
    private final PortfolioMapper portfolioMapper;
    private final PortfolioProfileMapper portfolioProfileMapper;
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
        if (applicationMapper.existsByRecruitmentAndApplicant(recruitmentId, freelancerEmail)) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        List<Integer> portfolioIds = validatePortfolioIds(request, freelancerEmail);
        ApplicationDTO application = ApplicationDTO.builder()
                .recruitmentId(recruitment.getRecruitmentId())
                .recruitmentTitle(recruitment.getTitle())
                .applicantEmail(freelancerEmail)
                .intro(normalizeIntro(request == null ? null : request.getIntro()))
                .status(ApplicationStatus.PENDING)
                .build();

        try {
            applicationMapper.insert(application);
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }
        portfolioPermissionMapper.insertAll(application.getApplicationId(), portfolioIds);
        return getMine(recruitmentId, freelancerEmail, role);
    }

    public ApplicationDetailResponse getMine(int recruitmentId, String freelancerEmail, String role) {
        requireUser(role);
        ApplicationDTO application = findMyApplication(recruitmentId, freelancerEmail);
        return toDetailResponse(application, false);
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

        portfolioPermissionMapper.deleteByApplicationId(application.getApplicationId());
        portfolioPermissionMapper.insertAll(application.getApplicationId(), portfolioIds);
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
        List<Integer> portfolioIds =
                portfolioPermissionMapper.findPortfolioIdsByApplicationId(application.getApplicationId());
        List<ApplicationPortfolioSummaryResponse> portfolios = portfolioIds == null || portfolioIds.isEmpty()
                ? List.of()
                : portfolioMapper.findApplicationSummariesByIds(portfolioIds);
        PortfolioProfileDTO portfolioProfile = includePortfolioProfile
                ? findPortfolioProfile(application.getApplicantEmail())
                : null;

        return ApplicationDetailResponse.builder()
                .applicationId(application.getApplicationId())
                .recruitmentId(application.getRecruitmentId())
                .contractId(application.getContractId())
                .recruitmentTitle(application.getRecruitmentTitle())
                .applicantEmail(application.getApplicantEmail())
                .applicantName(application.getApplicantName())
                .intro(application.getIntro())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .canceledAt(application.getCanceledAt())
                .viewedAt(application.getViewedAt())
                .portfolioProfile(portfolioProfile)
                .portfolios(portfolios == null ? List.of() : portfolios)
                .build();
    }

    private PortfolioProfileDTO findPortfolioProfile(String applicantEmail) {
        PortfolioProfileDTO profile = portfolioProfileMapper.findByFreelancerEmail(applicantEmail);
        if (profile != null) {
            profile.setTechStacks(portfolioProfileMapper.findTechStacks(applicantEmail));
        }
        return profile;
    }
}
