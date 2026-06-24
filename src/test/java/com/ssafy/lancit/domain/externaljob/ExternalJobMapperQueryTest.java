package com.ssafy.lancit.domain.externaljob;

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
    }

    @Test
    @DisplayName("기본 조회는 NOT_FREELANCE, EXCLUDED, 마감 지난 공고를 제외한다")
    void findExternalJobs_appliesDefaultVisibilityFilters() throws IOException {
        String xml = resource("mapper/ExternalJobMapper.xml");

        assertThat(xml).contains("freelance_type &lt;&gt; 'NOT_FREELANCE'");
        assertThat(xml).contains("recommendation_type &lt;&gt; 'EXCLUDED'");
        assertThat(xml).contains("condition.includeExpired == false");
        assertThat(xml).contains("deadline_at IS NULL");
        assertThat(xml).contains("deadline_at &gt;= NOW()");
    }

    @Test
    @DisplayName("DDL은 LONGTEXT payload와 source/source_job_id 유니크 키를 가진다")
    void ddl_containsExternalJobTable() throws IOException {
        String sql = resource("new_sql.sql");

        assertThat(sql).contains("CREATE TABLE external_job");
        assertThat(sql).contains("original_payload_json   LONGTEXT");
        assertThat(sql).contains("UNIQUE KEY uk_external_job_source_job_id (source, source_job_id)");
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
