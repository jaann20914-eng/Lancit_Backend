package com.ssafy.lancit.domain.externaljob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCategoryRecommendationCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectionLogCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobImportResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUserRecommendationCommand;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobCsvImportService;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalJobCsvImportServiceTest {

    @Test
    @DisplayName("공고 원본 CSV는 source/source_job_id 기준으로 external_job에 upsert한다")
    void importCsv_externalJobCsvUpsertsJobs() {
        FakeMapper mapper = new FakeMapper();
        ExternalJobCsvImportService service = new ExternalJobCsvImportService(mapper, new ObjectMapper());
        MockMultipartFile file = csv("""
                source,source_job_id,title,company_name,location,wage,deadline,original_url,description,job_category,freelance_type,recommendation_type,is_visible
                SEOUL,H954202606231080,IT컨설턴트 구인,주식회사이비즈앤컴,서울 금천구,최소연봉 / 2600만원,2026-08-22,https://example.com,ERP 컨설팅 및 IT 프로젝트 관리,컴퓨터시스템 설계 및 분석가,PROJECT_LIKE,HIGHLY_RECOMMENDED,1
                """);

        ExternalJobImportResponse response = service.importCsv(file);

        assertThat(response.getImportType()).isEqualTo("EXTERNAL_JOB");
        assertThat(response.getInsertedCount()).isEqualTo(1);
        assertThat(response.getFailedCount()).isZero();
        assertThat(mapper.upsertedJobs).hasSize(1);
        ExternalJobUpsertCommand command = mapper.upsertedJobs.get(0);
        assertThat(command.getSource()).isEqualTo(ExternalJobSource.SEOUL);
        assertThat(command.getSourceJobId()).isEqualTo("H954202606231080");
        assertThat(command.getFreelanceType()).isEqualTo(ExternalFreelanceType.PROJECT_LIKE);
        assertThat(command.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.HIGHLY_RECOMMENDED);
        assertThat(command.getVisible()).isTrue();
    }

    @Test
    @DisplayName("추천 결과 CSV는 없는 공고를 실패 목록에 남기고 나머지 import를 계속한다")
    void importCsv_recommendationCsvSkipsMissingJobs() {
        FakeMapper mapper = new FakeMapper();
        mapper.jobsBySourceKey.put("SEOUL:H954202606231080", ExternalJobDTO.builder()
                .id(10L)
                .source(ExternalJobSource.SEOUL)
                .sourceJobId("H954202606231080")
                .build());
        ExternalJobCsvImportService service = new ExternalJobCsvImportService(mapper, new ObjectMapper());
        MockMultipartFile file = csv("""
                job_category,source,source_job_id,recommendation_type,recommendation_score,matched_by,reason
                IT,SEOUL,H954202606231080,HIGHLY_RECOMMENDED,95,LLM_PRECOMPUTED,IT 업무와 관련성이 높음
                디자인,SEOUL,UNKNOWN,EXCLUDED,20,LLM_PRECOMPUTED,디자인 직무와 관련성이 낮음
                """);

        ExternalJobImportResponse response = service.importCsv(file);

        assertThat(response.getImportType()).isEqualTo("CATEGORY_RECOMMENDATION");
        assertThat(response.getInsertedCount()).isEqualTo(1);
        assertThat(response.getSkippedCount()).isEqualTo(1);
        assertThat(response.getFailedCount()).isEqualTo(1);
        assertThat(response.getFailedRows().get(0).getReason()).isEqualTo("EXTERNAL_JOB_NOT_FOUND");
        assertThat(mapper.savedRecommendations).hasSize(1);
        assertThat(mapper.savedRecommendations.get(0).getExternalJobId()).isEqualTo(10L);
    }

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile(
                "file",
                "external-jobs.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private static class FakeMapper implements ExternalJobMapper {
        private final List<ExternalJobUpsertCommand> upsertedJobs = new ArrayList<>();
        private final List<ExternalJobCategoryRecommendationCommand> savedRecommendations = new ArrayList<>();
        private final Map<String, ExternalJobDTO> jobsBySourceKey = new HashMap<>();

        @Override
        public int upsertExternalJob(ExternalJobUpsertCommand command) {
            upsertedJobs.add(command);
            jobsBySourceKey.put(key(command.getSource(), command.getSourceJobId()), ExternalJobDTO.builder()
                    .id((long) upsertedJobs.size())
                    .source(command.getSource())
                    .sourceJobId(command.getSourceJobId())
                    .build());
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
        public List<ExternalJobDTO> findVisibleExternalJobsForRecommendation() {
            return List.of();
        }

        @Override
        public List<ExternalJobDTO> findExternalJobsForReclassification(ExternalJobSource source) {
            return List.of();
        }

        @Override
        public int upsertExternalJobUserRecommendation(ExternalJobUserRecommendationCommand command) {
            return 0;
        }

        @Override
        public int upsertExternalJobCategoryRecommendation(ExternalJobCategoryRecommendationCommand command) {
            savedRecommendations.add(command);
            return 1;
        }

        @Override
        public long countCategoryRecommendations(String jobCategory) {
            return 0;
        }

        @Override
        public int updateExternalJobClassification(Long id,
                                                   ExternalFreelanceType freelanceType,
                                                   ExternalJobRecommendationType recommendationType,
                                                   Integer recommendationScore,
                                                   Boolean visible,
                                                   String visibilityReason,
                                                   LocalDateTime updatedAt) {
            return 0;
        }

        @Override
        public ExternalJobDTO findById(Long id) {
            return null;
        }

        @Override
        public ExternalJobDTO findBySourceAndSourceJobId(ExternalJobSource source, String sourceJobId) {
            return jobsBySourceKey.get(key(source, sourceJobId));
        }

        @Override
        public int refreshVisibilityByPolicy(ExternalJobSource source,
                                             LocalDateTime now,
                                             LocalDateTime staleBefore) {
            return 0;
        }

        @Override
        public int insertCollectionLog(ExternalJobCollectionLogCommand command) {
            return 0;
        }

        @Override
        public int tryAcquireCollectionLock(ExternalJobSource source,
                                            String lockedBy,
                                            LocalDateTime lockedAt,
                                            LocalDateTime lockedUntil,
                                            LocalDateTime updatedAt) {
            return 0;
        }

        @Override
        public int releaseCollectionLock(ExternalJobSource source, String lockedBy, LocalDateTime releasedAt) {
            return 0;
        }

        private static String key(ExternalJobSource source, String sourceJobId) {
            return source.name() + ":" + sourceJobId;
        }
    }
}
