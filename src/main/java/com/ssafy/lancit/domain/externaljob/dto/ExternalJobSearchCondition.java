package com.ssafy.lancit.domain.externaljob.dto;

import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSort;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalJobSearchCondition {
    @Schema(description = "제목, 회사명, 설명 검색어")
    private String keyword;

    @Schema(description = "노출 대상 공고의 추천 분류", allowableValues = {"HIGHLY_RECOMMENDED", "RECOMMENDED", "POSSIBLE"})
    private ExternalJobRecommendationType recommendationType;

    @Schema(description = "로그인 유저별 추천 점수 조회에 사용할 직종")
    private String jobCategory;

    @Schema(hidden = true)
    private String userEmail;

    @Schema(description = "정렬", allowableValues = {"RECOMMENDED", "LATEST"}, defaultValue = "RECOMMENDED")
    private ExternalJobSort sort = ExternalJobSort.RECOMMENDED;

    public String getSafeSort() {
        return sort == null ? ExternalJobSort.RECOMMENDED.name() : sort.name();
    }
}
