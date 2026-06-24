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
@Schema(description = "외부 공고 상세 응답")
public class ExternalJobDetailResponse {
    private Long id;
    private Long externalJobId;
    private ExternalJobSource source;
    private String sourceLabel;
    private String sourceJobId;
    private String title;
    private String summary;
    private String content;
    private String description;
    private String requirements;
    private String companyName;
    private String location;
    private String workLocation;
    private String jobCategoryRaw;
    private String employmentTypeRaw;
    private String salaryText;
    private LocalDateTime postedAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime recruitmentStartAt;
    private LocalDateTime recruitmentEndAt;
    private LocalDateTime createdAt;
    private LocalDateTime collectedAt;
    private LocalDateTime updatedAt;
    private String detailButtonLabel;
    private String sourceUrl;
    private String sourceButtonLabel;
    private ExternalFreelanceType freelanceType;
    private ExternalJobRecommendationType recommendationType;
    private String recommendationLabel;
    private int applicantCount;
    private Boolean canApply;
    private Boolean isApplied;
    private Boolean isBookmarked;
    private String externalNotice;
}
