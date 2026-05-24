package com.ssafy.lancit.domain.portfolio.dto;

import lombok.*;

import java.time.LocalDateTime;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioDTO {
    private int portfolioId;
    private String email;
    private String title;
    private String content;
    private LocalDateTime workStartAt;
    private LocalDateTime workEndAt;
    private boolean isPublic;
    private Integer bannerFileId;
    // 결과물 파일 → FileDTO list (서비스에서 조립)
}