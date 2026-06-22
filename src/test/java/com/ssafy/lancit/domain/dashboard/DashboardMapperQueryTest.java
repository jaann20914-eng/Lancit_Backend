package com.ssafy.lancit.domain.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardMapperQueryTest {

    @Test
    @DisplayName("대시보드 mapper는 조회문만 가지며 모든 최근 목록을 SQL에서 최대 2개로 제한한다")
    void mapper_isReadOnlyAndLimitsEveryRecentList() throws IOException {
        String xml = mapperXml();

        assertThat(xml).doesNotContain("<insert", "<update", "<delete");
        assertThat(occurrences(xml, "LIMIT 2")).isEqualTo(7);
    }

    @Test
    @DisplayName("삭제·취소 데이터 제외 및 상태별 집계 조건이 SQL에 고정되어 있다")
    void mapper_filtersDeletedCancelledAndCountsStatuses() throws IOException {
        String xml = mapperXml();

        assertThat(xml).contains("c.status = 'IN_PROGRESS'");
        assertThat(xml).contains("c.status = 'WAITING'");
        assertThat(xml).contains("ra.status &lt;&gt; 'CANCELLED'");
        assertThat(xml).contains("r.is_deleted = 0");
        assertThat(xml).contains("r.status &lt;&gt; 'CANCELLED'");
        assertThat(xml).contains("p.is_deleted = 0");
    }

    @Test
    @DisplayName("최근 공고 지원자 수는 공고별 추가 조회가 아닌 단일 조건부 집계로 계산한다")
    void recruitmentApplicantCount_isAggregatedWithoutNPlusOne() throws IOException {
        String xml = mapperXml();

        assertThat(xml).contains("LEFT JOIN recruitment_application ra");
        assertThat(xml).contains("COUNT(CASE WHEN ra.status &lt;&gt; 'CANCELLED' THEN 1 END)");
        assertThat(xml).contains("GROUP BY r.recruitment_id");
    }

    private String mapperXml() throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("mapper/DashboardMapper.xml")) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int occurrences(String source, String target) {
        return (source.length() - source.replace(target, "").length()) / target.length();
    }
}
