package com.ssafy.lancit.domain.externaljob.dto;

import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSort;
import com.ssafy.lancit.global.enums.ExternalJobSource;
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

    @Schema(description = "외부 공고 출처", allowableValues = {"SEOUL", "GYEONGGI"})
    private ExternalJobSource source;

    @Schema(description = "추천 분류", allowableValues = {"HIGHLY_RECOMMENDED", "RECOMMENDED", "POSSIBLE", "EXCLUDED"})
    private ExternalJobRecommendationType recommendationType;

    @Schema(description = "정렬", allowableValues = {"LATEST", "DEADLINE"}, defaultValue = "LATEST")
    private ExternalJobSort sort = ExternalJobSort.LATEST;

    @Schema(description = "마감 지난 공고 포함 여부", defaultValue = "false")
    private Boolean includeExpired = false;

    public String getSafeSort() {
        return sort == null ? ExternalJobSort.LATEST.name() : sort.name();
    }

    public boolean isIncludeExpiredValue() {
        return Boolean.TRUE.equals(includeExpired);
    }
}
