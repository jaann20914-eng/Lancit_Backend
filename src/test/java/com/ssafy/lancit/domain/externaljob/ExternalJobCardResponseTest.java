package com.ssafy.lancit.domain.externaljob;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCardResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalJobCardResponseTest {

    @Test
    @DisplayName("외부 공고 카드 응답에는 내부 분류 사유, 점수, 찜, 지원 필드를 노출하지 않는다")
    void cardResponse_excludesInternalAndRecruitmentFields() {
        Set<String> fieldNames = Arrays.stream(ExternalJobCardResponse.class.getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toSet());

        assertThat(fieldNames).doesNotContain(
                "career",
                "education",
                "reason",
                "score",
                "confidence",
                "isBookmarked",
                "bookmarkCount",
                "applicationStatus");
    }
}
