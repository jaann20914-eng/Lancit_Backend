package com.ssafy.lancit.domain.recruitment.post.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentStatus;

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
@Schema(description = "공고 등록 요청")
public class RecruitmentCreateRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "공고 제목", example = "랜딩 페이지 제작 프리랜서 모집")
    private String title;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "공고 한줄 소개", example = "SaaS 랜딩 페이지를 함께 만들 분을 찾습니다.")
    private String summary;

    @NotBlank
    @Schema(description = "공고 내용")
    private String content;

    @Schema(description = "요구사항")
    private String requirements;

    @NotNull
    @Schema(description = "공고 분야")
    private JobCategory jobCategory;

    @NotNull
    @Schema(description = "공고 카테고리")
    private RecruitmentCategory recruitmentCategory;

    @Schema(description = "저장 상태. 생략 시 OPEN")
    private RecruitmentStatus status;

    @Size(max = 255)
    @Schema(description = "근무지", example = "서울 강남구 / 원격 병행")
    private String workLocation;

    @PositiveOrZero
    @Schema(description = "예상 예산", example = "3000000")
    private Integer budget;

    @Schema(description = "공고 이미지 파일 ID")
    private Integer imageFileId;

    @Schema(description = "예상 시작일")
    private LocalDateTime contractStartAt;

    @Schema(description = "예상 종료일")
    private LocalDateTime contractEndAt;

    @Schema(description = "모집 시작일. 생략 시 현재 시각")
    private LocalDateTime recruitmentStartAt;

    @Schema(description = "모집 종료일")
    private LocalDateTime recruitmentEndAt;

    @Schema(description = "기술 스택 태그")
    private List<@Size(max = 50) String> techStacks = new ArrayList<>();
}
