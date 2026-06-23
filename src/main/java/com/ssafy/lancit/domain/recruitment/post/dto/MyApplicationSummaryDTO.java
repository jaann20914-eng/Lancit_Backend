package com.ssafy.lancit.domain.recruitment.post.dto;

import com.ssafy.lancit.global.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyApplicationSummaryDTO {
    private Integer recruitmentId;
    private Integer applicationId;
    private ApplicationStatus status;
}
