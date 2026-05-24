package com.ssafy.lancit.domain.recruitment.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.lancit.global.enums.ApplicationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationDTO {
    private int applicationId;
    private int recruitmentId;
    private String applicantEmail;
    private LocalDateTime appliedAt;
    private ApplicationStatus status;
    private boolean isBookmarkedByCompany;
    private List<Integer> portfolioIds; // 열람 허용 포트폴리오 ID 목록
}