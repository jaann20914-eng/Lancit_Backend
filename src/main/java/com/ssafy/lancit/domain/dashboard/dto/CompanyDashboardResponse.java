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
public class CompanyDashboardResponse {

    private final Summary summary;
    private final List<RecentContract> recentContracts;
    private final List<RecentApplication> recentApplications;
    private final List<RecentRecruitment> recentRecruitments;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Summary {
        private long inProgressContractCount;
        private long receivedApplicationCount;
        private long recruitmentCount;
        private long proposedTalentCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecentContract {
        private int contractId;
        private int recruitmentId;
        private String title;
        private String freelancerName;
        private String freelancerEmail;
        private LocalDate deadline;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecentApplication {
        private int applicationId;
        private int recruitmentId;
        private String recruitmentTitle;
        private String applicantName;
        private String applicantEmail;
        private String jobCategory;
        private LocalDateTime appliedAt;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecentRecruitment {
        private int recruitmentId;
        private String title;
        private LocalDateTime createdAt;
        private long applicantCount;
        private String status;
    }
}
