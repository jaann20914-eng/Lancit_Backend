package com.ssafy.lancit.domain.recruitment.application.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDetailResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationPortfolioSummaryResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationRequest;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.PortfolioPermissionMapper;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final String ROLE_USER = "USER";
    private static final int MAX_INTRO_LENGTH = 1000;

    private final ApplicationMapper applicationMapper;
    private final PortfolioPermissionMapper portfolioPermissionMapper;
    private final PortfolioMapper portfolioMapper;
    private final RecruitmentMapper recruitmentMapper;

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
        return toDetailResponse(application);
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

    private void requireUser(String role) {
        if (!ROLE_USER.equals(role)) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
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

    private ApplicationDetailResponse toDetailResponse(ApplicationDTO application) {
        List<Integer> portfolioIds =
                portfolioPermissionMapper.findPortfolioIdsByApplicationId(application.getApplicationId());
        List<ApplicationPortfolioSummaryResponse> portfolios = portfolioIds == null || portfolioIds.isEmpty()
                ? List.of()
                : portfolioMapper.findApplicationSummariesByIds(portfolioIds);

        return ApplicationDetailResponse.builder()
                .applicationId(application.getApplicationId())
                .recruitmentId(application.getRecruitmentId())
                .recruitmentTitle(application.getRecruitmentTitle())
                .applicantEmail(application.getApplicantEmail())
                .intro(application.getIntro())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .canceledAt(application.getCanceledAt())
                .viewedAt(application.getViewedAt())
                .portfolios(portfolios == null ? List.of() : portfolios)
                .build();
    }
}
