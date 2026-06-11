package com.ssafy.lancit.domain.portfolio.dto;

import lombok.*;

import java.time.LocalDateTime;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioDTO {
    private Integer portfolioId;
    private String email;
    private String category;
    private String title;
    private String summary;
    private String content;
    private LocalDateTime workStartAt;
    private LocalDateTime workEndAt;
    private Boolean isPublic;
    private Integer bannerFileId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    // 결과물 파일 → FileDTO list (서비스에서 조립)
}
