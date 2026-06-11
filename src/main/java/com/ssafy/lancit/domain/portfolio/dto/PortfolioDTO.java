package com.ssafy.lancit.domain.portfolio.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.global.enums.PortfolioCategory;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioDTO {
    private Integer portfolioId;
    private String email;

    @NotNull(message = "프로젝트 카테고리는 필수입니다.")
    @Schema(description = "프로젝트 카테고리", requiredMode = Schema.RequiredMode.REQUIRED)
    private PortfolioCategory category;

    @NotBlank(message = "프로젝트 제목은 필수입니다.")
    @Schema(description = "프로젝트 제목", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "한줄 소개는 필수입니다.")
    @Size(max = 30, message = "한줄 소개는 30자 이하여야 합니다.")
    @Schema(description = "프로젝트 한줄 소개", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 30)
    private String summary;

    @NotBlank(message = "프로젝트 설명은 필수입니다.")
    @Schema(description = "프로젝트 설명", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    private LocalDateTime workStartAt;
    private LocalDateTime workEndAt;
    private Boolean isPublic;

    @Schema(description = "배너 파일 ID. 생략하거나 null이면 배너를 제거합니다.", types = {"integer", "null"},
            format = "int32", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer bannerFileId;

    private FileDTO bannerFile;
    private List<Integer> fileIds;
    private List<FileDTO> files;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private Boolean owner;

    public boolean isOwner() {
        return Boolean.TRUE.equals(owner);
    }
}
