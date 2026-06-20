package com.ssafy.lancit.domain.recruitment.application.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.JobCategory;

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
public class ApplicationProfileSnapshotDTO {
    private Integer applicationId;
    private String displayName;
    private JobCategory jobCategory;
    private Integer profileFileId;
    private String intro;
    private String description;
    private Boolean isPortfolioPublic;
    private LocalDateTime sourceProfileUpdatedAt;
    private LocalDateTime createdAt;
}
