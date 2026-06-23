package com.ssafy.lancit.domain.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.lancit.common.exception.GlobalExceptionHandler;
import com.ssafy.lancit.domain.dashboard.controller.DashboardController;
import com.ssafy.lancit.domain.dashboard.dto.CompanyDashboardResponse;
import com.ssafy.lancit.domain.dashboard.dto.FreelancerDashboardResponse;
import com.ssafy.lancit.domain.dashboard.mapper.DashboardMapper;
import com.ssafy.lancit.domain.dashboard.service.DashboardService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DashboardService dashboardService = new DashboardService(new EmptyDashboardMapper());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardController(dashboardService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("회사가 프리랜서 대시보드에 접근하면 HTTP 403 envelope를 반환한다")
    void freelancerDashboard_companyRole_returns403() throws Exception {
        authenticate("company@test.com", "COMPANY");

        mockMvc.perform(get("/api/dashboard/freelancer"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("프리랜서가 회사 대시보드에 접근하면 HTTP 403 envelope를 반환한다")
    void companyDashboard_userRole_returns403() throws Exception {
        authenticate("user@test.com", "USER");

        mockMvc.perform(get("/api/dashboard/company"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("빈 프리랜서 대시보드는 공통 envelope 안에 0과 빈 배열을 반환한다")
    void freelancerDashboard_empty_returnsEnvelope() throws Exception {
        authenticate("user@test.com", "USER");

        mockMvc.perform(get("/api/dashboard/freelancer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.inProgressContractCount").value(0))
                .andExpect(jsonPath("$.data.summary.receivedProposalCount").value(0))
                .andExpect(jsonPath("$.data.recentContracts").isEmpty())
                .andExpect(jsonPath("$.data.recentProposals").isEmpty())
                .andExpect(jsonPath("$.data.recentApplications").isEmpty())
                .andExpect(jsonPath("$.data.recentPortfolios").isEmpty());
    }

    @Test
    @DisplayName("날짜와 isPublic 필드는 요구된 ISO-8601 JSON 이름으로 직렬화된다")
    void freelancerDashboard_serializesPresentationNeutralFields() throws Exception {
        EmptyDashboardMapper mapper = new EmptyDashboardMapper() {
            @Override
            public List<FreelancerDashboardResponse.RecentPortfolio> findRecentFreelancerPortfolios(
                    String email) {
                FreelancerDashboardResponse.RecentPortfolio portfolio =
                        new FreelancerDashboardResponse.RecentPortfolio();
                portfolio.setPortfolioId(1);
                portfolio.setTitle("포트폴리오");
                portfolio.setSummary("요약");
                portfolio.setCategory("WEB_APP");
                portfolio.setUpdatedAt(LocalDateTime.of(2026, 6, 10, 10, 0));
                portfolio.setIsPublic(true);
                return List.of(portfolio);
            }
        };
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardController(new DashboardService(mapper)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate("user@test.com", "USER");

        mockMvc.perform(get("/api/dashboard/freelancer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentPortfolios[0].category").value("WEB_APP"))
                .andExpect(jsonPath("$.data.recentPortfolios[0].updatedAt")
                        .value("2026-06-10T10:00:00"))
                .andExpect(jsonPath("$.data.recentPortfolios[0].isPublic").value(true))
                .andExpect(jsonPath("$.data.recentPortfolios[0].public").doesNotExist());
    }

    private void authenticate(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static class EmptyDashboardMapper implements DashboardMapper {

        @Override
        public FreelancerDashboardResponse.Summary findFreelancerSummary(String email) {
            return new FreelancerDashboardResponse.Summary();
        }

        @Override
        public List<FreelancerDashboardResponse.RecentContract> findRecentFreelancerContracts(String email) {
            return List.of();
        }

        @Override
        public List<FreelancerDashboardResponse.RecentProposal> findRecentFreelancerProposals(String email) {
            return List.of();
        }

        @Override
        public List<FreelancerDashboardResponse.RecentApplication> findRecentFreelancerApplications(String email) {
            return List.of();
        }

        @Override
        public List<FreelancerDashboardResponse.RecentPortfolio> findRecentFreelancerPortfolios(String email) {
            return List.of();
        }

        @Override
        public CompanyDashboardResponse.Summary findCompanySummary(String email) {
            return new CompanyDashboardResponse.Summary();
        }

        @Override
        public List<CompanyDashboardResponse.RecentContract> findRecentCompanyContracts(String email) {
            return List.of();
        }

        @Override
        public List<CompanyDashboardResponse.RecentApplication> findRecentCompanyApplications(String email) {
            return List.of();
        }

        @Override
        public List<CompanyDashboardResponse.RecentRecruitment> findRecentCompanyRecruitments(String email) {
            return List.of();
        }
    }
}
