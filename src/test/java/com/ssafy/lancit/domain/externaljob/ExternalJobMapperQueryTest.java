package com.ssafy.lancit.domain.externaljob;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.global.enums.ExternalJobSort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalJobMapperQueryTest {

    @Test
    @DisplayName("upsert는 source/sourceJobId 유니크 기준으로 중복 insert 대신 update한다")
    void upsert_usesDuplicateKeyUpdate() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");

        assertThat(xml).contains("ON DUPLICATE KEY UPDATE");
        assertThat(xml).contains("title = VALUES(title)");
        assertThat(xml).contains("recommendation_type = VALUES(recommendation_type)");
        assertThat(xml).contains("recommendation_score = VALUES(recommendation_score)");
    }

    @Test
    @DisplayName("기본 조회는 서울시 공고만 노출하고 is_visible 기준으로 비노출 공고를 제외한다")
    void findExternalJobs_appliesDefaultVisibilityFilters() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");

        assertThat(xml).contains("AND source = 'SEOUL'");
        assertThat(xml).doesNotContain("condition.source");
        assertThat(occurrences(xml, "AND source = 'SEOUL'")).isEqualTo(2);
        assertThat(xml).contains("AND is_visible = 1");
        assertThat(xml).contains("freelance_type = 'NOT_FREELANCE'");
        assertThat(xml).contains("recommendation_type = 'EXCLUDED'");
        assertThat(xml).contains("visibility_reason");
    }

    @Test
    @DisplayName("외부 공고 추천순 조회는 추천 점수, 추천 타입, 등록/수집일 순으로 정렬한다")
    void findExternalJobs_ordersByRecommendation() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");

        assertThat(xml).contains("recommendation_score DESC");
        assertThat(xml).contains("WHEN 'HIGHLY_RECOMMENDED' THEN 1");
        assertThat(xml).contains("WHEN 'RECOMMENDED' THEN 2");
        assertThat(xml).contains("WHEN 'POSSIBLE' THEN 3");
        assertThat(xml).contains("posted_at DESC");
        assertThat(xml).contains("collected_at DESC");
        assertThat(xml).doesNotContain("condition.safeSort == 'DEADLINE'");
    }

    @Test
    @DisplayName("외부 공고 최신순 조회는 등록/수집일 순으로 정렬한다")
    void findExternalJobs_ordersByLatest() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");

        assertThat(xml).contains("condition.safeSort == 'LATEST'");
        assertThat(xml).contains("posted_at DESC");
        assertThat(xml).contains("collected_at DESC");
    }

    @Test
    @DisplayName("외부 공고 정렬 파라미터는 기본 추천순이며 최신순 값을 유지한다")
    void searchCondition_usesSafeSort() {
        ExternalJobSearchCondition condition = new ExternalJobSearchCondition();
        condition.setSort(ExternalJobSort.LATEST);

        assertThat(new ExternalJobSearchCondition().getSafeSort()).isEqualTo("RECOMMENDED");
        assertThat(condition.getSafeSort()).isEqualTo("LATEST");
    }

    @Test
    @DisplayName("DDL은 LONGTEXT payload, 추천 점수, visibility, source/source_job_id 유니크 키를 가진다")
    void ddl_containsExternalJobTable() throws IOException {
        String sql = resource("new_sql.sql");

        assertThat(sql).contains("CREATE TABLE external_job");
        assertThat(sql).contains("original_payload_json   LONGTEXT");
        assertThat(sql).contains("recommendation_score    INT             NOT NULL    DEFAULT 0");
        assertThat(sql).contains("is_visible              TINYINT(1)      NOT NULL    DEFAULT 1");
        assertThat(sql).contains("visibility_reason       VARCHAR(80)     NOT NULL    DEFAULT 'VISIBLE'");
        assertThat(sql).contains("UNIQUE KEY uk_external_job_source_job_id (source, source_job_id)");
        assertThat(sql).contains("INDEX idx_external_job_visibility");
        assertThat(sql).contains("INDEX idx_external_job_recommendation_order");
    }

    @Test
    @DisplayName("DDL은 외부 공고 수집 로그와 실행 락 테이블을 가진다")
    void ddl_containsCollectionLogAndLockTables() throws IOException {
        String sql = resource("new_sql.sql");

        assertThat(sql).contains("CREATE TABLE external_job_collection_log");
        assertThat(sql).contains("status                  VARCHAR(30)");
        assertThat(sql).contains("first_failed_page       INT             NULL");
        assertThat(sql).contains("CREATE TABLE external_job_collection_lock");
        assertThat(sql).contains("PRIMARY KEY (source)");
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int occurrences(String source, String target) {
        return (source.length() - source.replace(target, "").length()) / target.length();
    }
}
