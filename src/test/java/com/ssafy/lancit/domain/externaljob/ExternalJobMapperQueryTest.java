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
        assertThat(xml).contains("INSERT INTO external_job_user_recommendation");
        assertThat(xml).contains("ON DUPLICATE KEY UPDATE");
        assertThat(xml).contains("matched_by = VALUES(matched_by)");
        assertThat(xml).contains("INSERT INTO external_job_category_recommendation");
        assertThat(xml).contains("reason = VALUES(reason)");
    }

    @Test
    @DisplayName("기본 조회는 직종별 사전 추천 결과가 있는 외부 공고만 노출한다")
    void findExternalJobs_appliesDefaultVisibilityFilters() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");
        String listQuery = section(xml,
                "<select id=\"findExternalJobs\"",
                "</select>");
        String countQuery = section(xml,
                "<select id=\"countExternalJobs\"",
                "</select>");
        String requiredJoin = section(xml,
                "<sql id=\"CategoryRecommendationRequiredJoin\"",
                "</sql>");

        assertThat(xml).doesNotContain("condition.source");
        assertThat(xml).contains("AND ej.is_visible = 1");
        assertThat(xml).contains("AND ej.freelance_type != 'NOT_FREELANCE'");
        assertThat(xml).doesNotContain("AND ej.recommendation_type != 'EXCLUDED'");
        assertThat(xml).contains("freelance_type = 'NOT_FREELANCE'");
        assertThat(xml).contains("visibility_reason");
        assertThat(listQuery).contains("<include refid=\"CategoryRecommendationRequiredJoin\"/>");
        assertThat(countQuery).contains("<include refid=\"CategoryRecommendationRequiredJoin\"/>");
        assertThat(requiredJoin).contains("INNER JOIN external_job_category_recommendation ejcr");
        assertThat(requiredJoin).doesNotContain("user_email");
        assertThat(requiredJoin).contains("ejcr.job_category = #{condition.jobCategory}");
        assertThat(requiredJoin).contains("ejcr.recommendation_type != 'EXCLUDED'");
        assertThat(requiredJoin).doesNotContain("ejcr.recommendation_score &gt;= 60");
        assertThat(requiredJoin).doesNotContain("ejcr.recommendation_score >= 60");
        assertThat(listQuery).doesNotContain("COALESCE(ejcr.recommendation_score, ej.recommendation_score, 0)");
    }

    @Test
    @DisplayName("사전 계산 후보는 노출 가능한 외부 공고 중 NOT_FREELANCE만 제외하고 추천 등급으로 제한하지 않는다")
    void findVisibleExternalJobsForRecommendation_usesFreelanceTypeOnly() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");
        String query = section(xml,
                "<select id=\"findVisibleExternalJobsForRecommendation\"",
                "</select>");

        assertThat(query).doesNotContain("AND ej.source = 'SEOUL'");
        assertThat(query).contains("AND ej.is_visible = 1");
        assertThat(query).contains("AND ej.freelance_type != 'NOT_FREELANCE'");
        assertThat(query).doesNotContain("AND ej.recommendation_type");
        assertThat(query).doesNotContain("HIGHLY_RECOMMENDED");
        assertThat(query).doesNotContain("RECOMMENDED");
        assertThat(query).doesNotContain("LIMIT");
    }

    @Test
    @DisplayName("외부 공고 조회는 직종별 사전 추천 점수로만 추천순 정렬한다")
    void findExternalJobs_ordersByRecommendation() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");

        assertThat(xml).contains("INNER JOIN external_job_category_recommendation ejcr");
        assertThat(xml).contains("ejcr.recommendation_score DESC");
        assertThat(xml).doesNotContain("LEFT JOIN external_job_category_recommendation ejcr");
        assertThat(xml).doesNotContain("COALESCE(ejcr.recommendation_score, ej.recommendation_score, 0)");
        assertThat(xml).contains("ej.id DESC");
        assertThat(xml).doesNotContain("condition.safeSort == 'DEADLINE'");
    }

    @Test
    @DisplayName("외부 공고 목록 쿼리는 최신순 분기 없이 추천순으로 고정한다")
    void findExternalJobs_ignoresLatestSortBranch() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");

        assertThat(xml).doesNotContain("condition.safeSort == 'LATEST'");
        assertThat(xml).contains("ORDER BY");
        assertThat(xml).contains("ejcr.recommendation_score DESC");
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
    @DisplayName("DDL은 외부 공고 유저별 추천 점수 테이블을 가진다")
    void ddl_containsExternalJobUserRecommendationTable() throws IOException {
        String sql = resource("new_sql.sql");

        assertThat(sql).contains("CREATE TABLE external_job_user_recommendation");
        assertThat(sql).contains("UNIQUE KEY uk_external_job_user_category");
        assertThat(sql).contains("INDEX idx_external_job_user_category");
        assertThat(sql).contains("FOREIGN KEY (external_job_id)");
        assertThat(sql).contains("REFERENCES external_job(id)");
        assertThat(sql).contains("ON DELETE CASCADE");
    }

    @Test
    @DisplayName("DDL은 외부 공고 직종별 사전 추천 테이블을 가진다")
    void ddl_containsExternalJobCategoryRecommendationTable() throws IOException {
        String sql = resource("new_sql.sql");

        assertThat(sql).contains("DROP TABLE IF EXISTS external_job_category_recommendation");
        assertThat(sql).contains("CREATE TABLE external_job_category_recommendation");
        assertThat(sql).contains("UNIQUE KEY uk_external_job_category_recommendation");
        assertThat(sql).contains("INDEX idx_external_job_category_score");
        assertThat(sql).contains("INDEX idx_external_job_category_type_score");
        assertThat(sql).contains("CONSTRAINT fk_external_job_category_recommendation_job");
        assertThat(sql).contains("REFERENCES external_job(id)");
        assertThat(sql).contains("ON DELETE CASCADE");
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

    private String section(String text, String startToken, String endToken) {
        int start = text.indexOf(startToken);
        assertThat(start).isGreaterThanOrEqualTo(0);
        int end = text.indexOf(endToken, start);
        assertThat(end).isGreaterThan(start);
        return text.substring(start, end + endToken.length());
    }
}
