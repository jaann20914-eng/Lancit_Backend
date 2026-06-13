package com.ssafy.lancit.domain.recruitment.post.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "공고 수정 요청")
public class RecruitmentUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 100)
    private String summary;

    @NotBlank
    private String content;

    private String requirements;

    @NotNull
    private JobCategory jobCategory;

    @NotNull
    private RecruitmentCategory recruitmentCategory;

    @Size(max = 255)
    private String workLocation;

    @PositiveOrZero
    private Integer budget;

    private Integer imageFileId;
    private LocalDateTime contractStartAt;
    private LocalDateTime contractEndAt;
    private LocalDateTime recruitmentStartAt;
    private LocalDateTime recruitmentEndAt;
    private List<@Size(max = 50) String> techStacks = new ArrayList<>();
}
