package com.ssafy.lancit.domain.externaljob.dto;

import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "외부 공고 카드 응답")
public class ExternalJobCardResponse {
    private Long id;
    private ExternalJobSource source;
    private String sourceLabel;
    private String title;
    private String companyName;
    private String location;
    private String jobCategoryRaw;
    private String employmentTypeRaw;
    private String salaryText;
    private LocalDateTime deadlineAt;
    private String sourceUrl;
    private ExternalFreelanceType freelanceType;
    private ExternalJobRecommendationType recommendationType;
    private String recommendationLabel;
    private LocalDateTime collectedAt;
}
