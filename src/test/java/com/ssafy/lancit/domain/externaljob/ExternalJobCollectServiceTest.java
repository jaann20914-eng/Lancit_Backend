package com.ssafy.lancit.domain.externaljob;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.externaljob.classifier.ExternalJobClassifier;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResult;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectionLogCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.domain.externaljob.normalizer.SeoulExternalJobNormalizer;
import com.ssafy.lancit.domain.externaljob.provider.ExternalJobProvider;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobCollectService;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobCollectionStatus;
import com.ssafy.lancit.global.enums.ExternalJobCollectionType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalJobCollectServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-23T15:00:00Z"), SEOUL_ZONE);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SeoulExternalJobNormalizer normalizer = new SeoulExternalJobNormalizer(objectMapper, FIXED_CLOCK);
    private final ExternalJobClassifier classifier = input -> ExternalJobClassification.builder()
            .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
            .recommendationType(ExternalJobRecommendationType.RECOMMENDED)
            .recommendationScore(77)
            .build();

    @Test
    @DisplayName("첫 페이지 수집이 실패하면 재시도 후 FAILED로 기록하고 기존 visibility를 갱신하지 않는다")
    void collectSeoulJobs_firstPageFailure_doesNotRefreshVisibility() {
        FakeProvider provider = new FakeProvider(objectMapper);
        provider.failStartIndexes.add(1);
        FakeMapper mapper = new FakeMapper();
        ExternalJobCollectService service = service(provider, mapper);

        ExternalJobCollectResponse response = service.collectSeoulJobs(
                ExternalJobCollectCommand.builder().page(1).size(100).maxPages(3).build(),
                ExternalJobCollectionType.DAILY);

        assertThat(response.getStatus()).isEqualTo(ExternalJobCollectionStatus.FAILED);
        assertThat(response.getFirstFailedPage()).isEqualTo(1);
        assertThat(response.getFailedPages()).isEqualTo(1);
        assertThat(provider.callsForStartIndex(1)).isEqualTo(4);
        assertThat(mapper.upsertedCommands).isEmpty();
        assertThat(mapper.visibilityRefreshCount).isZero();
        assertThat(mapper.logs).hasSize(1);
        assertThat(mapper.logs.get(0).getStatus()).isEqualTo(ExternalJobCollectionStatus.FAILED);
    }

    @Test
    @DisplayName("일부 페이지만 성공하면 성공분은 저장하고 PARTIAL_SUCCESS로 기록한다")
    void collectSeoulJobs_partialSuccess_savesSucceededRows() {
        FakeProvider provider = new FakeProvider(objectMapper);
        provider.failStartIndexes.add(101);
        FakeMapper mapper = new FakeMapper();
        ExternalJobCollectService service = service(provider, mapper);

        ExternalJobCollectResponse response = service.collectSeoulJobs(
                ExternalJobCollectCommand.builder().page(1).size(100).maxPages(3).build(),
                ExternalJobCollectionType.DAILY);

        assertThat(response.getStatus()).isEqualTo(ExternalJobCollectionStatus.PARTIAL_SUCCESS);
        assertThat(response.getSucceededPages()).isEqualTo(1);
        assertThat(response.getFailedPages()).isEqualTo(1);
        assertThat(response.getFirstFailedPage()).isEqualTo(2);
        assertThat(response.getUpsertedCount()).isEqualTo(1);
        assertThat(provider.callsForStartIndex(101)).isEqualTo(4);
        assertThat(mapper.upsertedCommands).hasSize(1);
        assertThat(mapper.upsertedCommands.get(0).getVisible()).isTrue();
        assertThat(mapper.visibilityRefreshCount).isEqualTo(1);
        assertThat(mapper.logs).hasSize(1);
        assertThat(mapper.logs.get(0).getStatus()).isEqualTo(ExternalJobCollectionStatus.PARTIAL_SUCCESS);
    }

    @Test
    @DisplayName("수집 저장 전 Gemini/fallback 분류 결과의 추천 타입과 점수를 정규화한다")
    void collectSeoulJobs_normalizesClassifierResultBeforeUpsert() {
        FakeProvider provider = new FakeProvider(objectMapper);
        FakeMapper mapper = new FakeMapper();
        ExternalJobClassifier inconsistentClassifier = input -> ExternalJobClassification.builder()
                .freelanceType(ExternalFreelanceType.NOT_FREELANCE)
                .recommendationType(ExternalJobRecommendationType.RECOMMENDED)
                .recommendationScore(99)
                .build();
        ExternalJobCollectService service = service(provider, mapper, inconsistentClassifier);

        ExternalJobCollectResponse response = service.collectSeoulJobs(
                ExternalJobCollectCommand.builder().page(1).size(100).maxPages(1).build(),
                ExternalJobCollectionType.DAILY);

        assertThat(response.getStatus()).isEqualTo(ExternalJobCollectionStatus.SUCCESS);
        assertThat(mapper.upsertedCommands).hasSize(1);
        ExternalJobUpsertCommand command = mapper.upsertedCommands.get(0);
        assertThat(command.getFreelanceType()).isEqualTo(ExternalFreelanceType.NOT_FREELANCE);
        assertThat(command.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.EXCLUDED);
        assertThat(command.getRecommendationScore()).isZero();
        assertThat(command.getVisible()).isFalse();
        assertThat(command.getVisibilityReason()).isEqualTo("NOT_FREELANCE");
    }

    private ExternalJobCollectService service(FakeProvider provider, FakeMapper mapper) {
        return service(provider, mapper, classifier);
    }

    private ExternalJobCollectService service(FakeProvider provider,
                                             FakeMapper mapper,
                                             ExternalJobClassifier classifier) {
        return new ExternalJobCollectService(List.of(provider), normalizer, classifier, mapper, FIXED_CLOCK);
    }

    private static class FakeProvider implements ExternalJobProvider {
        private final ObjectMapper objectMapper;
        private final List<Integer> failStartIndexes = new ArrayList<>();
        private final List<Integer> calledStartIndexes = new ArrayList<>();

        private FakeProvider(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public ExternalJobSource getSource() {
            return ExternalJobSource.SEOUL;
        }

        @Override
        public ExternalJobCollectResult collect(ExternalJobCollectCommand command) {
            int startIndex = command.getStartIndex();
            calledStartIndexes.add(startIndex);
            if (failStartIndexes.contains(startIndex)) {
                return ExternalJobCollectResult.builder()
                        .source(ExternalJobSource.SEOUL)
                        .failedCount(1)
                        .message("페이지 실패")
                        .rawRows(List.of())
                        .build();
            }
            if (startIndex > 1) {
                return ExternalJobCollectResult.builder()
                        .source(ExternalJobSource.SEOUL)
                        .message("빈 페이지")
                        .rawRows(List.of())
                        .build();
            }
            return ExternalJobCollectResult.builder()
                    .source(ExternalJobSource.SEOUL)
                    .fetchedCount(1)
                    .message("성공")
                    .rawRows(List.of(row()))
                    .build();
        }

        private int callsForStartIndex(int startIndex) {
            return (int) calledStartIndexes.stream()
                    .filter(calledStartIndex -> calledStartIndex == startIndex)
                    .count();
        }

        private JsonNode row() {
            ObjectNode row = objectMapper.createObjectNode();
            row.put("JO_REQST_NO", "SEOUL-001");
            row.put("JO_SJ", "IT 프로젝트 PM");
            row.put("CMPNY_NM", "서울시");
            row.put("JOBCODE_NM", "컴퓨터시스템 설계 및 분석가");
            row.put("EMPLYM_STLE_CMMN_MM", "계약직");
            row.put("WORK_PARAR_BASS_ADRES_CN", "서울");
            row.put("HOPE_WAGE", "월 500만원");
            row.put("JO_REG_DT", "20260620");
            row.put("RCEPT_CLOS_DT", "2026.07.10");
            row.put("DTY_CN", "공공기관 IT 프로젝트 PM 업무");
            return row;
        }
    }

    private static class FakeMapper implements ExternalJobMapper {
        private final List<ExternalJobUpsertCommand> upsertedCommands = new ArrayList<>();
        private final List<ExternalJobCollectionLogCommand> logs = new ArrayList<>();
        private int visibilityRefreshCount;

        @Override
        public int upsertExternalJob(ExternalJobUpsertCommand command) {
            upsertedCommands.add(command);
            return 1;
        }

        @Override
        public List<ExternalJobDTO> findExternalJobs(ExternalJobSearchCondition condition, PageRequest pageRequest) {
            return List.of();
        }

        @Override
        public long countExternalJobs(ExternalJobSearchCondition condition) {
            return 0;
        }

        @Override
        public ExternalJobDTO findById(Long id) {
            return null;
        }

        @Override
        public int refreshVisibilityByPolicy(ExternalJobSource source,
                                             LocalDateTime now,
                                             LocalDateTime staleBefore) {
            visibilityRefreshCount++;
            return 0;
        }

        @Override
        public int insertCollectionLog(ExternalJobCollectionLogCommand command) {
            logs.add(command);
            return 1;
        }

        @Override
        public int tryAcquireCollectionLock(ExternalJobSource source,
                                            String lockedBy,
                                            LocalDateTime lockedAt,
                                            LocalDateTime lockedUntil,
                                            LocalDateTime updatedAt) {
            return 1;
        }

        @Override
        public int releaseCollectionLock(ExternalJobSource source, String lockedBy, LocalDateTime releasedAt) {
            return 1;
        }
    }
}
