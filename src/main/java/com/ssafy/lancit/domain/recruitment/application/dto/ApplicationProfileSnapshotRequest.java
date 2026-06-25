package com.ssafy.lancit.domain.recruitment.application.dto;

import java.util.List;

import com.ssafy.lancit.global.enums.JobCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "공고 지원 전용 프로필 카드 스냅샷 요청")
public class ApplicationProfileSnapshotRequest {

    @NotBlank
    @Size(max = 100)
    private String displayName;

    @NotNull
    private JobCategory jobCategory;

    private Integer profileFileId;

    private Boolean isPortfolioPublic;

    @Size(max = 30)
    private String intro;

    @Size(max = 200)
    private String description;

    private List<@Size(max = 100) String> techStacks;
}
