package com.ssafy.lancit.domain.externaljob.dto;

import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobUserRecommendationCommand {
    private String userEmail;
    private Long externalJobId;
    private String jobCategory;
    private ExternalJobRecommendationType recommendationType;
    private Integer recommendationScore;
    private String matchedBy;
}
