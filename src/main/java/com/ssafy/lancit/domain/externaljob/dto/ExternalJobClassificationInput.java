package com.ssafy.lancit.domain.externaljob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobClassificationInput {
    private String title;
    private String companyName;
    private String description;
    private String jobCategoryRaw;
    private String employmentTypeRaw;
    private String location;
    private String salaryRaw;
}
