package com.ssafy.lancit.domain.recruitment.application.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 공고 지원 상세 응답")
public class ApplicationDetailResponse {
    private Integer applicationId;
    private Integer recruitmentId;
    private String recruitmentTitle;
    private String applicantEmail;
    private String applicantName;
    private String intro;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime viewedAt;
    private PortfolioProfileDTO portfolioProfile;

    @Builder.Default
    private List<ApplicationPortfolioSummaryResponse> portfolios = new ArrayList<>();
}
