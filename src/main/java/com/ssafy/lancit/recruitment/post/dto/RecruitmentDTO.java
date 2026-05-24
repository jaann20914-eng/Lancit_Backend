package com.ssafy.lancit.recruitment.post.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecruitmentDTO {
    private int recruitmentId;
    private String email;               // 작성 회사 이메일
    private String title;
    private String content;
    private JobCategory jobCategory;
    private RecruitmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime contractStartAt;
    private LocalDateTime contractEndAt;
    private int budget;
}
