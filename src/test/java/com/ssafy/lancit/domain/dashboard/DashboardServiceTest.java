package com.ssafy.lancit.domain.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.dashboard.dto.CompanyDashboardResponse;
import com.ssafy.lancit.domain.dashboard.dto.FreelancerDashboardResponse;
import com.ssafy.lancit.domain.dashboard.mapper.DashboardMapper;
import com.ssafy.lancit.domain.dashboard.service.DashboardService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final String USER_EMAIL = "user@lancit.com";
    private static final String COMPANY_EMAIL = "company@lancit.com";

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private DashboardMapper dashboardMapper;

    @Test
    @DisplayName("프리랜서는 자신의 이메일로 집계와 최근 목록만 조회한다")
    void freelancerDashboard_queriesOnlyOwnedData() {
        FreelancerDashboardResponse.Summary summary = freelancerSummary(1, 2, 3, 4);
        given(dashboardMapper.findFreelancerSummary(USER_EMAIL)).willReturn(summary);
        given(dashboardMapper.findRecentFreelancerContracts(USER_EMAIL)).willReturn(List.of());
        given(dashboardMapper.findRecentFreelancerProposals(USER_EMAIL)).willReturn(List.of());
        given(dashboardMapper.findRecentFreelancerApplications(USER_EMAIL)).willReturn(List.of());
        given(dashboardMapper.findRecentFreelancerPortfolios(USER_EMAIL)).willReturn(List.of());

        FreelancerDashboardResponse response =
                dashboardService.getFreelancerDashboard(USER_EMAIL, "USER");

        assertThat(response.getSummary().getInProgressContractCount()).isEqualTo(1);
        assertThat(response.getSummary().getReceivedProposalCount()).isEqualTo(2);
        assertThat(response.getSummary().getAppliedRecruitmentCount()).isEqualTo(3);
        assertThat(response.getSummary().getPortfolioCount()).isEqualTo(4);
        verify(dashboardMapper).findFreelancerSummary(USER_EMAIL);
        verify(dashboardMapper).findRecentFreelancerContracts(USER_EMAIL);
        verify(dashboardMapper).findRecentFreelancerProposals(USER_EMAIL);
        verify(dashboardMapper).findRecentFreelancerApplications(USER_EMAIL);
        verify(dashboardMapper).findRecentFreelancerPortfolios(USER_EMAIL);
        verify(dashboardMapper, never()).findCompanySummary(USER_EMAIL);
    }

    @Test
    @DisplayName("회사는 자신의 이메일로 집계와 최근 목록만 조회한다")
    void companyDashboard_queriesOnlyOwnedData() {
        CompanyDashboardResponse.Summary summary = companySummary(2, 3, 4, 5);
        given(dashboardMapper.findCompanySummary(COMPANY_EMAIL)).willReturn(summary);
        given(dashboardMapper.findRecentCompanyContracts(COMPANY_EMAIL)).willReturn(List.of());
        given(dashboardMapper.findRecentCompanyApplications(COMPANY_EMAIL)).willReturn(List.of());
        given(dashboardMapper.findRecentCompanyRecruitments(COMPANY_EMAIL)).willReturn(List.of());

        CompanyDashboardResponse response =
                dashboardService.getCompanyDashboard(COMPANY_EMAIL, "COMPANY");

        assertThat(response.getSummary().getInProgressContractCount()).isEqualTo(2);
        assertThat(response.getSummary().getReceivedApplicationCount()).isEqualTo(3);
        assertThat(response.getSummary().getRecruitmentCount()).isEqualTo(4);
        assertThat(response.getSummary().getProposedTalentCount()).isEqualTo(5);
        verify(dashboardMapper).findCompanySummary(COMPANY_EMAIL);
        verify(dashboardMapper).findRecentCompanyContracts(COMPANY_EMAIL);
        verify(dashboardMapper).findRecentCompanyApplications(COMPANY_EMAIL);
        verify(dashboardMapper).findRecentCompanyRecruitments(COMPANY_EMAIL);
        verify(dashboardMapper, never()).findFreelancerSummary(COMPANY_EMAIL);
    }

    @Test
    @DisplayName("다른 역할의 대시보드 접근은 403 예외이며 DB 조회도 하지 않는다")
    void roleMismatch_isForbiddenWithoutQuery() {
        assertThatThrownBy(() -> dashboardService.getFreelancerDashboard(COMPANY_EMAIL, "COMPANY"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FREELANCER_ONLY));
        assertThatThrownBy(() -> dashboardService.getCompanyDashboard(USER_EMAIL, "USER"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RECRUITMENT_COMPANY_ONLY));

        assertThat(ErrorCode.FREELANCER_ONLY.getStatus().value()).isEqualTo(403);
        assertThat(ErrorCode.RECRUITMENT_COMPANY_ONLY.getStatus().value()).isEqualTo(403);
        verifyNoInteractions(dashboardMapper);
    }

    @Test
    @DisplayName("데이터가 없으면 집계 0과 null이 아닌 빈 배열을 반환한다")
    void emptyData_returnsZeroAndEmptyLists() {
        given(dashboardMapper.findFreelancerSummary(USER_EMAIL))
                .willReturn(freelancerSummary(0, 0, 0, 0));
        given(dashboardMapper.findRecentFreelancerContracts(USER_EMAIL)).willReturn(null);
        given(dashboardMapper.findRecentFreelancerProposals(USER_EMAIL)).willReturn(null);
        given(dashboardMapper.findRecentFreelancerApplications(USER_EMAIL)).willReturn(null);
        given(dashboardMapper.findRecentFreelancerPortfolios(USER_EMAIL)).willReturn(null);

        FreelancerDashboardResponse response =
                dashboardService.getFreelancerDashboard(USER_EMAIL, "USER");

        assertThat(response.getSummary().getInProgressContractCount()).isZero();
        assertThat(response.getSummary().getReceivedProposalCount()).isZero();
        assertThat(response.getSummary().getAppliedRecruitmentCount()).isZero();
        assertThat(response.getSummary().getPortfolioCount()).isZero();
        assertThat(response.getRecentContracts()).isEmpty();
        assertThat(response.getRecentProposals()).isEmpty();
        assertThat(response.getRecentApplications()).isEmpty();
        assertThat(response.getRecentPortfolios()).isEmpty();
    }

    @Test
    @DisplayName("최근 목록은 mapper가 반환한 최신순 최대 2개를 그대로 유지한다")
    void recentRecruitments_keepsLatestTwoOrder() {
        CompanyDashboardResponse.RecentRecruitment newest = recruitment(3,
                LocalDateTime.of(2026, 6, 20, 10, 0));
        CompanyDashboardResponse.RecentRecruitment second = recruitment(2,
                LocalDateTime.of(2026, 6, 19, 10, 0));
        given(dashboardMapper.findCompanySummary(COMPANY_EMAIL))
                .willReturn(companySummary(0, 0, 0, 0));
        given(dashboardMapper.findRecentCompanyContracts(COMPANY_EMAIL)).willReturn(List.of());
        given(dashboardMapper.findRecentCompanyApplications(COMPANY_EMAIL)).willReturn(List.of());
        given(dashboardMapper.findRecentCompanyRecruitments(COMPANY_EMAIL))
                .willReturn(List.of(newest, second));

        CompanyDashboardResponse response =
                dashboardService.getCompanyDashboard(COMPANY_EMAIL, "COMPANY");

        assertThat(response.getRecentRecruitments())
                .extracting(CompanyDashboardResponse.RecentRecruitment::getRecruitmentId)
                .containsExactly(3, 2);
        assertThat(response.getRecentRecruitments()).hasSize(2);
    }

    @Test
    @DisplayName("대시보드 서비스의 모든 조회는 readOnly 트랜잭션이다")
    void serviceTransaction_isReadOnly() {
        Transactional transactional = DashboardService.class.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    private FreelancerDashboardResponse.Summary freelancerSummary(
            long contracts, long proposals, long applications, long portfolios) {
        FreelancerDashboardResponse.Summary summary = new FreelancerDashboardResponse.Summary();
        summary.setInProgressContractCount(contracts);
        summary.setReceivedProposalCount(proposals);
        summary.setAppliedRecruitmentCount(applications);
        summary.setPortfolioCount(portfolios);
        return summary;
    }

    private CompanyDashboardResponse.Summary companySummary(
            long contracts, long applications, long recruitments, long proposedTalents) {
        CompanyDashboardResponse.Summary summary = new CompanyDashboardResponse.Summary();
        summary.setInProgressContractCount(contracts);
        summary.setReceivedApplicationCount(applications);
        summary.setRecruitmentCount(recruitments);
        summary.setProposedTalentCount(proposedTalents);
        return summary;
    }

    private CompanyDashboardResponse.RecentRecruitment recruitment(int id, LocalDateTime createdAt) {
        CompanyDashboardResponse.RecentRecruitment recruitment =
                new CompanyDashboardResponse.RecentRecruitment();
        recruitment.setRecruitmentId(id);
        recruitment.setCreatedAt(createdAt);
        return recruitment;
    }
}
