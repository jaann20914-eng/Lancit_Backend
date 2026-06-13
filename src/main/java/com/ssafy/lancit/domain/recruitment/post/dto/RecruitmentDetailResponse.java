package com.ssafy.lancit.domain.recruitment.post.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import com.ssafy.lancit.global.enums.RecruitmentViewStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공고 상세 응답")
public class RecruitmentDetailResponse {
    private int recruitmentId;
    private String title;
    private String summary;
    private String content;
    private String requirements;
    private String companyEmail;
    private String companyName;
    private Integer imageFileId;
    private JobCategory jobCategory;
    private RecruitmentCategory recruitmentCategory;
    @Builder.Default
    private List<String> techStacks = new ArrayList<>();
    private String workLocation;
    private int budget;
    private RecruitmentStatus status;
    private RecruitmentViewStatus viewStatus;
    private LocalDateTime recruitmentStartAt;
    private LocalDateTime recruitmentEndAt;
    private LocalDateTime contractStartAt;
    private LocalDateTime contractEndAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int applicantCount;
    private Boolean isMine;
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canChangeStatus;
    private Boolean canApply;
    private Boolean isApplied;
    private Boolean isBookmarked;
    private RecruitmentPermissionResponse permission;
}
