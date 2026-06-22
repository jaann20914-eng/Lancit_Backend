package com.ssafy.lancit.domain.dashboard.mapper;

import com.ssafy.lancit.domain.dashboard.dto.CompanyDashboardResponse;
import com.ssafy.lancit.domain.dashboard.dto.FreelancerDashboardResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardMapper {

    FreelancerDashboardResponse.Summary findFreelancerSummary(@Param("email") String email);

    List<FreelancerDashboardResponse.RecentContract> findRecentFreelancerContracts(
            @Param("email") String email);

    List<FreelancerDashboardResponse.RecentProposal> findRecentFreelancerProposals(
            @Param("email") String email);

    List<FreelancerDashboardResponse.RecentApplication> findRecentFreelancerApplications(
            @Param("email") String email);

    List<FreelancerDashboardResponse.RecentPortfolio> findRecentFreelancerPortfolios(
            @Param("email") String email);

    CompanyDashboardResponse.Summary findCompanySummary(@Param("email") String email);

    List<CompanyDashboardResponse.RecentContract> findRecentCompanyContracts(
            @Param("email") String email);

    List<CompanyDashboardResponse.RecentApplication> findRecentCompanyApplications(
            @Param("email") String email);

    List<CompanyDashboardResponse.RecentRecruitment> findRecentCompanyRecruitments(
            @Param("email") String email);
}
