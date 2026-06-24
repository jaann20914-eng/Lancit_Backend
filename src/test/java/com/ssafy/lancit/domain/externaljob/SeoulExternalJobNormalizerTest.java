package com.ssafy.lancit.domain.externaljob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.domain.externaljob.normalizer.SeoulExternalJobNormalizer;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SeoulExternalJobNormalizerTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SeoulExternalJobNormalizer normalizer = new SeoulExternalJobNormalizer(
            objectMapper,
            Clock.fixed(Instant.parse("2026-06-23T15:00:00Z"), SEOUL_ZONE));

    @Test
    @DisplayName("서울시 row를 내부 external_job upsert command로 변환한다")
    void normalize_success() {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("JO_REQST_NO", "SEOUL-001");
        row.put("JO_SJ", "웹 서비스 개발 프로젝트");
        row.put("CMPNY_NM", "랜싯랩");
        row.put("RCRIT_JSSFC_CMMN_CODE_SE_NM", "웹 개발자");
        row.put("EMPLYM_STLE_CMMN_CODE_SE_NM", "계약직");
        row.put("WORK_PARAR_BASS_ADRES_CN", "서울시 강남구");
        row.put("HOPE_WAGE", "월 300만원");
        row.put("JO_REG_DT", "20260620");
        row.put("RCEPT_CLOS_DT", "2026.07.10");
        row.put("BSNS_SUMRY_CN", "프로젝트 단위 웹 개발 업무");

        Optional<ExternalJobUpsertCommand> result = normalizer.normalize(row);

        assertThat(result).isPresent();
        ExternalJobUpsertCommand command = result.get();
        assertThat(command.getSource()).isEqualTo(ExternalJobSource.SEOUL);
        assertThat(command.getSourceJobId()).isEqualTo("SEOUL-001");
        assertThat(command.getTitle()).isEqualTo("웹 서비스 개발 프로젝트");
        assertThat(command.getCompanyName()).isEqualTo("랜싯랩");
        assertThat(command.getJobCategoryRaw()).isEqualTo("웹 개발자");
        assertThat(command.getEmploymentTypeRaw()).isEqualTo("계약직");
        assertThat(command.getLocation()).isEqualTo("서울시 강남구");
        assertThat(command.getDeadlineAt()).isEqualTo(LocalDateTime.of(2026, 7, 10, 0, 0));
        assertThat(command.getPayloadHash()).hasSize(64);
        assertThat(command.getCollectedAt()).isEqualTo(LocalDateTime.of(2026, 6, 24, 0, 0));
    }

    @Test
    @DisplayName("sourceJobId가 없으면 저장하지 않는다")
    void normalize_missingSourceJobId_skip() {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("JO_SJ", "콘텐츠 편집 프로젝트");

        assertThat(normalizer.normalize(row)).isEmpty();
    }

    @Test
    @DisplayName("title이 없으면 회사명과 모집직종명으로 대체 title을 만든다")
    void normalize_missingTitle_usesCompanyAndCategory() {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("구인등록번호", "SEOUL-002");
        row.put("기업명칭", "서울디자인");
        row.put("모집직종명", "영상 편집자");

        Optional<ExternalJobUpsertCommand> result = normalizer.normalize(row);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("서울디자인 영상 편집자");
    }

    @Test
    @DisplayName("title 대체 생성도 불가능하면 저장하지 않는다")
    void normalize_missingTitleAndFallback_skip() {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("JO_REQST_NO", "SEOUL-003");

        assertThat(normalizer.normalize(row)).isEmpty();
    }
}
