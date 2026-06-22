package com.ssafy.lancit.domain.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecruitmentMapperQueryTest {

    @Test
    @DisplayName("ALL 탭 기본 OPEN 조건은 명시적 상태 필터가 없을 때만 적용한다")
    void allTabDefaultFilter_doesNotConflictWithExplicitStatus() throws IOException {
        String xml = mapperXml();

        assertThat(xml).contains(
                "condition.safeTab == 'ALL' and condition.status == null");
        assertThat(xml).contains("condition.status == 'EXPIRED'");
        assertThat(xml).contains("condition.status == 'CLOSED'");
        assertThat(xml).contains("condition.status == 'CANCELLED'");
    }

    private String mapperXml() throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("mapper/RecruitmentMapper.xml")) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
