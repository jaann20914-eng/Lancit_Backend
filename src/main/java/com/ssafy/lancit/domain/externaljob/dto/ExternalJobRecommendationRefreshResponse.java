package com.ssafy.lancit.domain.externaljob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobRecommendationRefreshResponse {
    private String jobCategory;
    private int refreshedCount;
}
