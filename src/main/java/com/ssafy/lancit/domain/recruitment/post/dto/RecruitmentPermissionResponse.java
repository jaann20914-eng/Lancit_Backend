package com.ssafy.lancit.domain.recruitment.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현재 조회자의 공고 권한")
public class RecruitmentPermissionResponse {
    private Boolean isMine;
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canChangeStatus;
    private Boolean canApply;
    private Boolean isApplied;
    private Boolean isBookmarked;
}
