package com.ssafy.lancit.domain.externaljob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCategoryRecommendationCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectionLogCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationPrecomputeRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationPrecomputeResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationRefreshResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUserRecommendationCommand;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobRecommendationService;
import com.ssafy.lancit.domain.externaljob.service.GeminiExternalJobPersonalRecommendationClient;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalJobRecommendationServiceTest {

    @Test
    @DisplayName("refresh는 사전 계산 결과만 확인하고 Gemini나 유저별 저장을 수행하지 않는다")
    void refreshPersonalRecommendations_usesPrecomputedStatusOnly() {
        FakeMapper mapper = new FakeMapper(List.of());
        mapper.precomputedCount = 3;
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(
                mapper,
                availableProvider(List.of()));

        ExternalJobRecommendationRefreshResponse response =
                service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertThat(response.getStatus()).isEqualTo("READY");
        assertThat(response.getMessage()).contains("사전 계산");
        assertThat(response.getRefreshedCount()).isZero();
        assertThat(response.getPrecomputedCount()).isEqualTo(3);
        assertThat(mapper.savedCategoryCommands).isEmpty();
        assertThat(mapper.savedUserCommands).isEmpty();
    }

    @Test
    @DisplayName("refresh는 사전 계산 결과가 없으면 NOT_FOUND 상태를 즉시 반환한다")
    void refreshPersonalRecommendations_returnsNotFoundWhenPrecomputedMissing() {
        FakeMapper mapper = new FakeMapper(List.of());
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, unavailableProvider());

        ExternalJobRecommendationRefreshResponse response =
                service.refreshPersonalRecommendations(null, "디자인");

        assertThat(response.getStatus()).isEqualTo("PRECOMPUTED_RECOMMENDATION_NOT_FOUND");
        assertThat(response.getPrecomputedCount()).isZero();
        assertThat(mapper.savedCategoryCommands).isEmpty();
    }

    @Test
    @DisplayName("precompute는 Gemini가 없으면 fallback으로 직종별 추천 테이블에 저장한다")
    void precomputeCategoryRecommendations_usesFallbackWhenGeminiUnavailable() {
        FakeMapper mapper = new FakeMapper(List.of(
                job(1L, "웹 개발 프로젝트", "IT 개발", "React 프론트엔드 개발", 70),
                job(2L, "콘텐츠 편집", "콘텐츠", "영상 편집 업무", 45)));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, unavailableProvider());

        ExternalJobRecommendationPrecomputeResponse response = service.precomputeCategoryRecommendations(
                ExternalJobRecommendationPrecomputeRequest.builder()
                        .jobCategories(List.of("IT"))
                        .build());

        assertThat(response.getProcessedJobCount()).isEqualTo(2);
        assertThat(response.getProcessedCategoryCount()).isEqualTo(1);
        assertThat(response.getSavedRecommendationCount()).isEqualTo(2);
        assertThat(response.getFailedCount()).isZero();
        assertThat(mapper.savedCategoryCommands)
                .extracting(ExternalJobCategoryRecommendationCommand::getJobCategory)
                .containsExactly("IT", "IT");
        assertThat(mapper.savedCategoryCommands)
                .extracting(ExternalJobCategoryRecommendationCommand::getMatchedBy)
                .containsOnly("FALLBACK_PRECOMPUTED");
        assertThat(mapper.savedUserCommands).isEmpty();
    }

    @Test
    @DisplayName("precompute는 단일 jobCategory 요청을 허용한다")
    void precomputeCategoryRecommendations_acceptsSingleJobCategory() {
        FakeMapper mapper = new FakeMapper(List.of(
                job(1L, "웹 개발 프로젝트", "IT 개발", "React 프론트엔드 개발", 70)));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, unavailableProvider());

        ExternalJobRecommendationPrecomputeResponse response = service.precomputeCategoryRecommendations(
                ExternalJobRecommendationPrecomputeRequest.builder()
                        .jobCategory(" IT ")
                        .build());

        assertThat(response.getProcessedCategoryCount()).isEqualTo(1);
        assertThat(mapper.savedCategoryCommands)
                .extracting(ExternalJobCategoryRecommendationCommand::getJobCategory)
                .containsExactly("IT");
    }

    @Test
    @DisplayName("precompute는 다중 직종의 공백과 중복을 제거해 한 번씩 처리한다")
    void precomputeCategoryRecommendations_deduplicatesResolvedJobCategories() {
        FakeMapper mapper = new FakeMapper(List.of(
                job(1L, "백엔드 개발 프로젝트", "IT 개발", "Spring 백엔드 개발", 70)));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, unavailableProvider());

        ExternalJobRecommendationPrecomputeResponse response = service.precomputeCategoryRecommendations(
                ExternalJobRecommendationPrecomputeRequest.builder()
                        .jobCategory("IT")
                        .jobCategories(List.of("IT", "IT", " 백엔드 개발자 ", " "))
                        .build());

        assertThat(response.getProcessedCategoryCount()).isEqualTo(2);
        assertThat(mapper.savedCategoryCommands)
                .extracting(ExternalJobCategoryRecommendationCommand::getJobCategory)
                .containsExactly("IT", "백엔드 개발자");
    }

    @Test
    @DisplayName("precompute는 Gemini 결과도 추천 타입별 점수 구간으로 보정해 저장한다")
    void precomputeCategoryRecommendations_normalizesGeminiScoreBeforeSaving() {
        FakeMapper mapper = new FakeMapper(List.of(
                candidate(1L),
                candidate(2L),
                candidate(3L)));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, availableProvider(List.of(
                personalRecommendation(1L, ExternalJobRecommendationType.POSSIBLE, 30),
                personalRecommendation(2L, ExternalJobRecommendationType.RECOMMENDED, 85),
                personalRecommendation(3L, ExternalJobRecommendationType.EXCLUDED, 70))));

        service.precomputeCategoryRecommendations(ExternalJobRecommendationPrecomputeRequest.builder()
                .jobCategories(List.of("IT"))
                .build());

        assertThat(mapper.savedCategoryCommands)
                .extracting(ExternalJobCategoryRecommendationCommand::getRecommendationType,
                        ExternalJobCategoryRecommendationCommand::getRecommendationScore,
                        ExternalJobCategoryRecommendationCommand::getMatchedBy)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.POSSIBLE, 40, "LLM_PRECOMPUTED"),
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.RECOMMENDED, 79, "LLM_PRECOMPUTED"),
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.EXCLUDED, 39, "LLM_PRECOMPUTED")
                );
    }

    @Test
    @DisplayName("precompute 요청 직종이 비어 있으면 INVALID_INPUT을 반환한다")
    void precomputeCategoryRecommendations_rejectsBlankCategories() {
        ExternalJobRecommendationService service =
                new ExternalJobRecommendationService(new FakeMapper(List.of()), unavailableProvider());

        assertThatThrownBy(() -> service.precomputeCategoryRecommendations(
                ExternalJobRecommendationPrecomputeRequest.builder()
                        .jobCategories(List.of(" "))
                        .build()))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private static ExternalJobDTO job(Long id, String title, String category, String description, int score) {
        return ExternalJobDTO.builder()
                .id(id)
                .title(title)
                .jobCategoryRaw(category)
                .description(description)
                .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                .recommendationType(score >= 60
                        ? ExternalJobRecommendationType.RECOMMENDED
                        : ExternalJobRecommendationType.POSSIBLE)
                .recommendationScore(score)
                .build();
    }

    private static ExternalJobDTO candidate(Long id) {
        return ExternalJobDTO.builder()
                .id(id)
                .title("서울시 업무지원 공고 " + id)
                .jobCategoryRaw("업무지원")
                .description("업무지원")
                .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                .recommendationType(ExternalJobRecommendationType.RECOMMENDED)
                .recommendationScore(77)
                .build();
    }

    private static ExternalJobPersonalRecommendation personalRecommendation(Long externalJobId,
                                                                            ExternalJobRecommendationType type,
                                                                            int score) {
        return ExternalJobPersonalRecommendation.builder()
                .externalJobId(externalJobId)
                .recommendationType(type)
                .recommendationScore(score)
                .matchedBy("GEMINI")
                .build();
    }

    private static ObjectProvider<GeminiExternalJobPersonalRecommendationClient> unavailableProvider() {
        return new ObjectProvider<>() {
            @Override
            public GeminiExternalJobPersonalRecommendationClient getObject(Object... args) {
                return null;
            }

            @Override
            public GeminiExternalJobPersonalRecommendationClient getIfAvailable() {
                return null;
            }

            @Override
            public GeminiExternalJobPersonalRecommendationClient getIfUnique() {
                return null;
            }

            @Override
            public GeminiExternalJobPersonalRecommendationClient getObject() {
                return null;
            }
        };
    }

    private static ObjectProvider<GeminiExternalJobPersonalRecommendationClient> availableProvider(
            List<ExternalJobPersonalRecommendation> recommendations) {
        GeminiExternalJobPersonalRecommendationClient client = new StubGeminiClient(recommendations);
        return new ObjectProvider<>() {
            @Override
            public GeminiExternalJobPersonalRecommendationClient getObject(Object... args) {
                return client;
            }

            @Override
            public GeminiExternalJobPersonalRecommendationClient getIfAvailable() {
                return client;
            }

            @Override
            public GeminiExternalJobPersonalRecommendationClient getIfUnique() {
                return client;
            }

            @Override
            public GeminiExternalJobPersonalRecommendationClient getObject() {
                return client;
            }
        };
    }

    private static class StubGeminiClient extends GeminiExternalJobPersonalRecommendationClient {
        private final List<ExternalJobPersonalRecommendation> recommendations;

        private StubGeminiClient(List<ExternalJobPersonalRecommendation> recommendations) {
            super(new ObjectMapper(), null);
            this.recommendations = recommendations;
        }

        @Override
        public List<ExternalJobPersonalRecommendation> recommend(String userJobCategory, List<ExternalJobDTO> jobs) {
            return recommendations;
        }
    }

    private static class FakeMapper implements ExternalJobMapper {
        private final List<ExternalJobDTO> jobs;
        private final List<ExternalJobCategoryRecommendationCommand> savedCategoryCommands = new ArrayList<>();
        private final List<ExternalJobUserRecommendationCommand> savedUserCommands = new ArrayList<>();
        private long precomputedCount;

        private FakeMapper(List<ExternalJobDTO> jobs) {
            this.jobs = jobs;
        }

        @Override
        public int upsertExternalJob(ExternalJobUpsertCommand command) {
            return 0;
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
            return jobs;
        }

        @Override
        public List<ExternalJobDTO> findExternalJobsForReclassification(ExternalJobSource source) {
            return List.of();
        }

        @Override
        public int upsertExternalJobUserRecommendation(ExternalJobUserRecommendationCommand command) {
            savedUserCommands.add(command);
            return 1;
        }

        @Override
        public int upsertExternalJobCategoryRecommendation(ExternalJobCategoryRecommendationCommand command) {
            savedCategoryCommands.add(command);
            return 1;
        }

        @Override
        public long countCategoryRecommendations(String jobCategory) {
            return precomputedCount;
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
            return null;
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
    }
}
