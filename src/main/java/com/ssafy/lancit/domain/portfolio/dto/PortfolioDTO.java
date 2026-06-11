package com.ssafy.lancit.domain.portfolio.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.lancit.domain.file.dto.FileDTO;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioDTO {
    private int portfolioId;
    private String email;
    private String category;
    private String title;
    private String summary;
    private String content;
    private LocalDateTime workStartAt;
    private LocalDateTime workEndAt;
    private Boolean isPublic;
    private Integer bannerFileId;
    private FileDTO bannerFile;
    private List<Integer> fileIds;
    private List<FileDTO> files;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private boolean owner;
}
