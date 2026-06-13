package com.ssafy.lancit.domain.bookmark.freelancer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공고 찜 토글 응답")
public class RecruitmentBookmarkResponse {
    private int recruitmentId;
    private Boolean isBookmarked;
}
