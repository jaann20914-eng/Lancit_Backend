package com.ssafy.lancit.domain.recruitment.post.dto;

import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentSortType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecruitmentSearchCondition {
    private String keyword;
    private JobCategory jobCategory;
    private RecruitmentCategory recruitmentCategory;
    private String status;
    private RecruitmentSortType sort = RecruitmentSortType.LATEST;

    public String getSafeSort() {
        return sort == null ? RecruitmentSortType.LATEST.name() : sort.name();
    }
}
