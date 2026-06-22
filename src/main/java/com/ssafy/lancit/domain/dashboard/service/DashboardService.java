package com.ssafy.lancit.domain.dashboard.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.dashboard.dto.CompanyDashboardResponse;
import com.ssafy.lancit.domain.dashboard.dto.FreelancerDashboardResponse;
import com.ssafy.lancit.domain.dashboard.mapper.DashboardMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_COMPANY = "COMPANY";

    private final DashboardMapper dashboardMapper;

    public FreelancerDashboardResponse getFreelancerDashboard(String email, String role) {
        requireRole(role, ROLE_USER, ErrorCode.FREELANCER_ONLY);
        return new FreelancerDashboardResponse(
                dashboardMapper.findFreelancerSummary(email),
                immutable(dashboardMapper.findRecentFreelancerContracts(email)),
                immutable(dashboardMapper.findRecentFreelancerProposals(email)),
                immutable(dashboardMapper.findRecentFreelancerApplications(email)),
                immutable(dashboardMapper.findRecentFreelancerPortfolios(email)));
    }

    public CompanyDashboardResponse getCompanyDashboard(String email, String role) {
        requireRole(role, ROLE_COMPANY, ErrorCode.RECRUITMENT_COMPANY_ONLY);
        return new CompanyDashboardResponse(
                dashboardMapper.findCompanySummary(email),
                immutable(dashboardMapper.findRecentCompanyContracts(email)),
                immutable(dashboardMapper.findRecentCompanyApplications(email)),
                immutable(dashboardMapper.findRecentCompanyRecruitments(email)));
    }

    private void requireRole(String actualRole, String expectedRole, ErrorCode errorCode) {
        if (!expectedRole.equals(actualRole)) {
            throw new CustomException(errorCode);
        }
    }

    private <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
