package com.ssafy.lancit.domain.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class FreelancerDashboardResponse {

    private final Summary summary;
    private final List<RecentContract> recentContracts;
    private final List<RecentProposal> recentProposals;
    private final List<RecentApplication> recentApplications;
    private final List<RecentPortfolio> recentPortfolios;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Summary {
        private long inProgressContractCount;
        private long receivedProposalCount;
        private long appliedRecruitmentCount;
        private long portfolioCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecentContract {
        private int contractId;
        private int recruitmentId;
        private String title;
        private String companyName;
        private String companyEmail;
        private LocalDate deadline;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecentProposal {
        private int contractId;
        private int recruitmentId;
        private String title;
        private String companyName;
        private String companyEmail;
        private LocalDateTime proposedAt;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecentApplication {
        private int applicationId;
        private int recruitmentId;
        private String title;
        private String companyName;
        private LocalDateTime appliedAt;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecentPortfolio {
        private int portfolioId;
        private String title;
        private String summary;
        private String category;
        private LocalDateTime updatedAt;
        private Boolean isPublic;
    }
}
