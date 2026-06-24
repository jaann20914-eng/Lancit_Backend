package com.ssafy.lancit.domain.externaljob.dto;

import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ExternalJobUpsertCommand {
    private ExternalJobSource source;
    private String sourceJobId;
    private String sourceUrl;
    private String title;
    private String companyName;
    private String location;
    private String jobCategoryRaw;
    private String employmentTypeRaw;
    private String salaryRaw;
    private LocalDateTime postedAt;
    private LocalDateTime deadlineAt;
    private String description;
    private String originalPayloadJson;
    private String payloadHash;
    private ExternalFreelanceType freelanceType;
    private ExternalJobRecommendationType recommendationType;
    private LocalDateTime collectedAt;
    private LocalDateTime updatedAt;
}
