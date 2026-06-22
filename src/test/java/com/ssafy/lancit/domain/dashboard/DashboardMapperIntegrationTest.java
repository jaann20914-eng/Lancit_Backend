package com.ssafy.lancit.domain.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.lancit.domain.dashboard.dto.CompanyDashboardResponse;
import com.ssafy.lancit.domain.dashboard.dto.FreelancerDashboardResponse;
import com.ssafy.lancit.domain.dashboard.mapper.DashboardMapper;
import com.ssafy.lancit.domain.dashboard.service.DashboardService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class DashboardMapperIntegrationTest {

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DashboardService dashboardService;
    private String userEmail;
    private String otherUserEmail;
    private String companyEmail;
    private String otherCompanyEmail;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        userEmail = "dashboard-user-" + suffix + "@test.com";
        otherUserEmail = "dashboard-other-user-" + suffix + "@test.com";
        companyEmail = "dashboard-company-" + suffix + "@test.com";
        otherCompanyEmail = "dashboard-other-company-" + suffix + "@test.com";
        dashboardService = new DashboardService(dashboardMapper);

        insertUser(userEmail, "대시보드 사용자");
        insertUser(otherUserEmail, "다른 사용자");
        insertCompany(companyEmail, "대시보드 회사");
        insertCompany(otherCompanyEmail, "다른 회사");
    }

    @Test
    @DisplayName("실제 DB에서 소유권·상태·삭제 필터와 최신순 2개 집계를 검증한다")
    void dashboardQueries_filterAndAggregateRealData() {
        int r1 = insertRecruitment(companyEmail, "공고 1", "OPEN", false, at(10));
        int r2 = insertRecruitment(companyEmail, "공고 2", "OPEN", false, at(11));
        int r3 = insertRecruitment(companyEmail, "공고 3", "CLOSED", false, at(12));
        int r4 = insertRecruitment(companyEmail, "공고 4", "OPEN", false, at(9));
        int cancelledRecruitment = insertRecruitment(
                companyEmail, "취소 공고", "CANCELLED", false, at(20));
        int deletedRecruitment = insertRecruitment(
                companyEmail, "삭제 공고", "OPEN", true, at(21));
        int otherRecruitment = insertRecruitment(
                otherCompanyEmail, "다른 회사 공고", "OPEN", false, at(8));

        insertApplication(r1, userEmail, "PENDING", at(10));
        insertApplication(r2, userEmail, "ACCEPTED", at(11));
        insertApplication(r3, userEmail, "REJECTED", at(12));
        insertApplication(r4, otherUserEmail, "PENDING", at(9));
        insertApplication(r4, userEmail, "CANCELLED", at(22));
        insertApplication(cancelledRecruitment, userEmail, "PENDING", at(23));
        insertApplication(deletedRecruitment, userEmail, "PENDING", at(24));
        insertApplication(otherRecruitment, userEmail, "PENDING", at(8));

        insertPortfolio(userEmail, "포트폴리오 1", false, at(10));
        insertPortfolio(userEmail, "포트폴리오 2", false, at(11));
        insertPortfolio(userEmail, "포트폴리오 3", false, at(12));
        insertPortfolio(userEmail, "삭제 포트폴리오", true, at(30));
        insertPortfolio(otherUserEmail, "다른 사용자 포트폴리오", false, at(29));

        int inProgress1 = insertContract(r1, companyEmail, userEmail, "IN_PROGRESS");
        int inProgress2 = insertContract(r2, companyEmail, userEmail, "IN_PROGRESS");
        int inProgress3 = insertContract(r3, companyEmail, userEmail, "IN_PROGRESS");
        insertContract(r4, companyEmail, otherUserEmail, "IN_PROGRESS");
        int otherCompanyContract = insertContract(
                otherRecruitment, otherCompanyEmail, userEmail, "IN_PROGRESS");
        insertContract(cancelledRecruitment, companyEmail, userEmail, "IN_PROGRESS");
        insertContract(deletedRecruitment, companyEmail, userEmail, "IN_PROGRESS");
        insertContract(r1, companyEmail, userEmail, "CANCELLED");
        insertContractDocument(inProgress1, LocalDate.of(2026, 7, 10), at(10));
        insertContractDocument(inProgress2, LocalDate.of(2026, 7, 11), at(11));
        insertContractDocument(inProgress3, LocalDate.of(2026, 7, 12), at(12));
        insertContractDocument(otherCompanyContract, LocalDate.of(2026, 7, 8), at(8));

        insertProposal(r1, companyEmail, userEmail, at(10));
        insertProposal(r2, companyEmail, userEmail, at(11));
        insertProposal(r3, companyEmail, userEmail, at(12));
        insertProposal(r4, companyEmail, otherUserEmail, at(9));
        insertProposal(otherRecruitment, otherCompanyEmail, userEmail, at(8));
        insertProposal(cancelledRecruitment, companyEmail, userEmail, at(30));
        insertProposal(deletedRecruitment, companyEmail, userEmail, at(29));

        FreelancerDashboardResponse freelancer =
                dashboardService.getFreelancerDashboard(userEmail, "USER");
        CompanyDashboardResponse company =
                dashboardService.getCompanyDashboard(companyEmail, "COMPANY");

        assertThat(freelancer.getSummary().getInProgressContractCount()).isEqualTo(4);
        assertThat(freelancer.getSummary().getReceivedProposalCount()).isEqualTo(4);
        assertThat(freelancer.getSummary().getAppliedRecruitmentCount()).isEqualTo(4);
        assertThat(freelancer.getSummary().getPortfolioCount()).isEqualTo(3);
        assertThat(freelancer.getRecentContracts()).hasSize(2);
        assertThat(freelancer.getRecentContracts())
                .extracting(FreelancerDashboardResponse.RecentContract::getRecruitmentId)
                .containsExactly(r3, r2);
        assertThat(freelancer.getRecentProposals())
                .extracting(FreelancerDashboardResponse.RecentProposal::getRecruitmentId)
                .containsExactly(r3, r2);
        assertThat(freelancer.getRecentApplications())
                .extracting(FreelancerDashboardResponse.RecentApplication::getRecruitmentId)
                .containsExactly(r3, r2);
        assertThat(freelancer.getRecentPortfolios())
                .extracting(FreelancerDashboardResponse.RecentPortfolio::getTitle)
                .containsExactly("포트폴리오 3", "포트폴리오 2");

        assertThat(company.getSummary().getInProgressContractCount()).isEqualTo(4);
        assertThat(company.getSummary().getReceivedApplicationCount()).isEqualTo(4);
        assertThat(company.getSummary().getRecruitmentCount()).isEqualTo(4);
        assertThat(company.getSummary().getProposedTalentCount()).isEqualTo(4);
        assertThat(company.getRecentContracts())
                .extracting(CompanyDashboardResponse.RecentContract::getRecruitmentId)
                .containsExactly(r3, r2);
        assertThat(company.getRecentApplications())
                .extracting(CompanyDashboardResponse.RecentApplication::getRecruitmentId)
                .containsExactly(r3, r2);
        assertThat(company.getRecentRecruitments())
                .extracting(CompanyDashboardResponse.RecentRecruitment::getRecruitmentId)
                .containsExactly(r3, r2);
        assertThat(company.getRecentRecruitments())
                .extracting(CompanyDashboardResponse.RecentRecruitment::getApplicantCount)
                .containsExactly(1L, 1L);
    }

    @Test
    @DisplayName("조회 API 서비스 실행 전후 관련 테이블 데이터 건수는 바뀌지 않는다")
    void dashboardRead_doesNotChangeDatabaseState() {
        Map<String, Long> before = tableCounts();

        FreelancerDashboardResponse freelancer =
                dashboardService.getFreelancerDashboard(userEmail, "USER");
        CompanyDashboardResponse company =
                dashboardService.getCompanyDashboard(companyEmail, "COMPANY");

        assertThat(freelancer.getSummary().getInProgressContractCount()).isZero();
        assertThat(freelancer.getRecentContracts()).isEmpty();
        assertThat(company.getSummary().getRecruitmentCount()).isZero();
        assertThat(company.getRecentRecruitments()).isEmpty();
        assertThat(tableCounts()).isEqualTo(before);
    }

    private void insertUser(String email, String name) {
        jdbcTemplate.update("""
                INSERT INTO user (email, password, name, phone, job_category)
                VALUES (?, 'password', ?, '010-0000-0000', 'IT')
                """, email, name);
    }

    private void insertCompany(String email, String companyName) {
        jdbcTemplate.update("""
                INSERT INTO company (email, password, name, company_name, phone, job_category)
                VALUES (?, 'password', '담당자', ?, '02-0000-0000', 'IT')
                """, email, companyName);
    }

    private int insertRecruitment(String email, String title, String status,
                                  boolean deleted, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO recruitment (
                    company_email, title, summary, job_category, recruitment_category,
                    status, created_at, recruitment_start_at, is_deleted, deleted_at
                ) VALUES (?, ?, '', 'IT', 'WEB_APP', ?, ?, ?, ?, ?)
                """, email, title, status, Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt),
                deleted, deleted ? Timestamp.valueOf(createdAt.plusDays(1)) : null);
        return jdbcTemplate.queryForObject(
                "SELECT recruitment_id FROM recruitment WHERE company_email = ? AND title = ?",
                Integer.class, email, title);
    }

    private void insertApplication(int recruitmentId, String applicantEmail,
                                   String status, LocalDateTime appliedAt) {
        jdbcTemplate.update("""
                INSERT INTO recruitment_application (
                    recruitment_id, applicant_email, intro, applied_at, status, canceled_at
                ) VALUES (?, ?, '', ?, ?, ?)
                """, recruitmentId, applicantEmail, Timestamp.valueOf(appliedAt), status,
                "CANCELLED".equals(status) ? Timestamp.valueOf(appliedAt.plusHours(1)) : null);
    }

    private void insertPortfolio(String email, String title, boolean deleted, LocalDateTime updatedAt) {
        jdbcTemplate.update("""
                INSERT INTO portfolio (
                    email, category, title, summary, is_public, created_at, updated_at,
                    is_deleted, deleted_at
                ) VALUES (?, 'WEB_APP', ?, '', 1, ?, ?, ?, ?)
                """, email, title, Timestamp.valueOf(updatedAt.minusDays(1)),
                Timestamp.valueOf(updatedAt), deleted,
                deleted ? Timestamp.valueOf(updatedAt.plusHours(1)) : null);
    }

    private int insertContract(int recruitmentId, String ownerCompanyEmail,
                               String freelancerEmail, String status) {
        jdbcTemplate.update("""
                INSERT INTO contract (recruitment_id, company_email, freelancer_email, status)
                VALUES (?, ?, ?, ?)
                """, recruitmentId, ownerCompanyEmail, freelancerEmail, status);
        return jdbcTemplate.queryForObject("SELECT MAX(contract_id) FROM contract", Integer.class);
    }

    private void insertContractDocument(int contractId, LocalDate deadline, LocalDateTime confirmedAt) {
        jdbcTemplate.update("""
                INSERT INTO contract_document (contract_id, contract_end_date, confirmed_at)
                VALUES (?, ?, ?)
                """, contractId, deadline, Timestamp.valueOf(confirmedAt));
    }

    private void insertProposal(int recruitmentId, String ownerCompanyEmail,
                                String freelancerEmail, LocalDateTime proposedAt) {
        int contractId = insertContract(
                recruitmentId, ownerCompanyEmail, freelancerEmail, "WAITING");
        jdbcTemplate.update("""
                INSERT INTO notification (receiver_email, type, target_id, is_read, created_at)
                VALUES (?, 'PROPOSAL', ?, 0, ?)
                """, freelancerEmail, contractId, Timestamp.valueOf(proposedAt));
    }

    private Map<String, Long> tableCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : List.of(
                "contract", "contract_document", "notification", "recruitment_application",
                "recruitment", "portfolio", "user", "company")) {
            counts.put(table, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table, Long.class));
        }
        return counts;
    }

    private LocalDateTime at(int day) {
        return LocalDateTime.of(2026, 6, day, 10, 0);
    }
}
