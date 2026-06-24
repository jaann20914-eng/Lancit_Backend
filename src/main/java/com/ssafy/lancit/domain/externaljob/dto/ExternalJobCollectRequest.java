package com.ssafy.lancit.domain.externaljob.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "외부 공고 수동 수집 요청")
public class ExternalJobCollectRequest {
    @Schema(description = "페이지 번호. startIndex/endIndex가 없을 때 사용", example = "1")
    private Integer page;

    @Schema(description = "페이지 크기. startIndex/endIndex가 없을 때 사용", example = "100")
    private Integer size;

    @Schema(description = "서울시 API 시작 인덱스", example = "1")
    private Integer startIndex;

    @Schema(description = "서울시 API 종료 인덱스", example = "100")
    private Integer endIndex;

    @Schema(description = "연속 수집할 최대 페이지 수", example = "1")
    private Integer maxPages;
}
