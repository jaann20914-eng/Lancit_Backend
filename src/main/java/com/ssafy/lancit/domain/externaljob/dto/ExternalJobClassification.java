package com.ssafy.lancit.domain.externaljob.dto;

import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobClassification {
    private ExternalFreelanceType freelanceType;
    private ExternalJobRecommendationType recommendationType;
    private String label;
    private Double confidence;
    private String reason;
}
