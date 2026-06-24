package com.ssafy.lancit.domain.externaljob.dto;

import com.ssafy.lancit.global.enums.ExternalJobSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "외부 공고 수집 결과")
public class ExternalJobCollectResponse {
    private ExternalJobSource source;
    private int fetchedCount;
    private int upsertedCount;
    private int skippedCount;
    private int failedCount;
    private String message;
}
