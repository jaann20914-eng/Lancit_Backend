package com.ssafy.lancit.domain.recruitment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "지원서에 연결된 포트폴리오 요약")
public class ApplicationPortfolioSummaryResponse {
    private Integer portfolioId;
    private String title;
    private String summary;
    private String category;
    private Integer bannerFileId;
    private Boolean isPublic;
}
