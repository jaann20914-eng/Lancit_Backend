package com.ssafy.lancit.domain.externaljob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobRecommendationPrecomputeResponse {
    private int processedJobCount;
    private int processedCategoryCount;
    private int savedRecommendationCount;
    private int failedCount;
}
