package com.ssafy.lancit.domain.recruitment.application.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.ApplicationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationDTO {
    private Integer applicationId;
    private Integer recruitmentId;
    private String recruitmentTitle;
    private String applicantEmail;
    private String applicantName;
    private String intro;
    private LocalDateTime appliedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime viewedAt;
    private ApplicationStatus status;
    private Boolean bookmarkedByCompany;
}
