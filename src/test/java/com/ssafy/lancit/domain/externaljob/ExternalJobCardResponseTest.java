package com.ssafy.lancit.domain.externaljob;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCardResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalJobCardResponseTest {

    @Test
    @DisplayName("외부 공고 조회 응답에는 화면 계약 필드만 노출한다")
    void externalJobResponses_exposeOnlyPublicContractFields() {
        Set<String> fieldNames = Arrays.stream(ExternalJobCardResponse.class.getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toSet());
        Set<String> detailFieldNames = Arrays.stream(ExternalJobDetailResponse.class.getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toSet());

        assertThat(fieldNames).doesNotContain(
                "career",
                "education",
                "reason",
                "score",
                "recommendationScore",
                "confidence",
                "isBookmarked",
                "bookmarkCount",
                "applicationStatus");
        assertThat(detailFieldNames).doesNotContain("recommendationScore", "reason", "confidence");
        assertThat(fieldNames).contains(
                "id",
                "externalJobId",
                "location",
                "workLocation",
                "detailButtonLabel",
                "sourceUrl",
                "sourceButtonLabel");
    }
}
