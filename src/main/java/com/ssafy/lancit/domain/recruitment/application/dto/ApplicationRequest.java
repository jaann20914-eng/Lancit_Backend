package com.ssafy.lancit.domain.recruitment.application.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "공고 지원 등록/수정 요청")
public class ApplicationRequest {

    @Size(max = 1000)
    @Schema(description = "해당 공고 지원 전용 소개", example = "백엔드 API 설계와 MyBatis 구현 경험을 살려 기여하겠습니다.")
    private String intro;

    @NotEmpty
    @Schema(description = "지원 시 열람을 허용할 내 포트폴리오 ID 목록", example = "[1, 3, 5]")
    private List<@NotNull Integer> portfolioIds = new ArrayList<>();

    @Valid
    @Schema(description = "지원용 프로필 카드 스냅샷. 없으면 현재 포트폴리오 프로필 카드로 스냅샷을 생성합니다.")
    private ApplicationProfileSnapshotRequest portfolioProfile;
}
