package com.ssafy.lancit.domain.recruitment.post.dto;

import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentSortType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecruitmentSearchCondition {
    private String keyword;
    @Schema(description = "구인페이지 탭", allowableValues = {"ALL", "APPLIED", "BOOKMARKED"})
    private String tab;
    private JobCategory jobCategory;
    private RecruitmentCategory recruitmentCategory;
    private String status;
    private RecruitmentSortType sort = RecruitmentSortType.LATEST;
    @Schema(hidden = true)
    private String currentEmail;

    public String getSafeSort() {
        return sort == null ? RecruitmentSortType.LATEST.name() : sort.name();
    }

    public String getSafeTab() {
        return tab;
    }
}
