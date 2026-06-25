package com.ssafy.lancit.domain.recruitment.post.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecruitmentDTO {
    private int recruitmentId;
    private String companyEmail;        // 작성 회사 이메일
    private String companyName;
    private String title;
    private String summary;
    private String content;
    private String requirements;
    private JobCategory jobCategory;
    private RecruitmentCategory recruitmentCategory;
    private RecruitmentStatus status;
    private String workLocation;
    private int budget;
    private Integer imageFileId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime contractStartAt;
    private LocalDateTime contractEndAt;
    private LocalDateTime recruitmentStartAt;
    private LocalDateTime recruitmentEndAt;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private int applicantCount;
    private Boolean businessNumberVerified;
}
