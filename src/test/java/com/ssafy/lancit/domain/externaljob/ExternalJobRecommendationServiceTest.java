package com.ssafy.lancit.domain.externaljob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectionLogCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationRefreshResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUserRecommendationCommand;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.domain.externaljob.service.GeminiExternalJobPersonalRecommendationClient;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobRecommendationService;
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
    @DisplayName("Gemini 클라이언트가 없어도 전역 점수 fallback으로 유저별 추천을 저장한다")
    void refreshPersonalRecommendations_usesFallbackWhenGeminiUnavailable() {
        FakeMapper mapper = new FakeMapper(List.of(
                ExternalJobDTO.builder()
                        .id(1L)
                        .title("웹 개발 프로젝트")
                        .jobCategoryRaw("IT 개발")
                        .description("프론트엔드 개발 업무")
                        .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                        .recommendationType(ExternalJobRecommendationType.RECOMMENDED)
                        .recommendationScore(70)
                        .build(),
                ExternalJobDTO.builder()
                        .id(2L)
                        .title("콘텐츠 편집")
                        .jobCategoryRaw("콘텐츠")
                        .description("영상 편집 업무")
                        .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                        .recommendationType(ExternalJobRecommendationType.POSSIBLE)
                        .recommendationScore(45)
                        .build()));
        ObjectProvider<GeminiExternalJobPersonalRecommendationClient> provider = unavailableProvider();
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, provider);

        ExternalJobRecommendationRefreshResponse response =
                service.refreshPersonalRecommendations("user@lancit.com", "IT/개발");

        assertThat(response.getJobCategory()).isEqualTo("IT/개발");
        assertThat(response.getRefreshedCount()).isEqualTo(2);
        assertThat(mapper.savedCommands).hasSize(2);
        assertThat(mapper.savedCommands)
                .extracting(ExternalJobUserRecommendationCommand::getMatchedBy)
                .containsOnly("FALLBACK");
        assertThat(mapper.savedCommands.get(0).getRecommendationScore()).isEqualTo(79);
        assertThat(mapper.savedCommands.get(1).getRecommendationScore()).isEqualTo(45);
    }

    @Test
    @DisplayName("fallback은 내부 직종 enum 키워드 매핑으로 서울시 직무 텍스트를 매칭한다")
    void refreshPersonalRecommendations_usesMappedKeywordsForInternalJobCategory() {
        FakeMapper mapper = new FakeMapper(List.of(
                ExternalJobDTO.builder()
                        .id(1L)
                        .title("프론트엔드 개발자 모집")
                        .jobCategoryRaw("컴퓨터시스템 설계 및 분석가")
                        .description("React 기반 관리자 웹 개발 업무")
                        .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                        .recommendationType(ExternalJobRecommendationType.RECOMMENDED)
                        .recommendationScore(70)
                        .build()));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, unavailableProvider());

        ExternalJobRecommendationRefreshResponse response =
                service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertThat(response.getJobCategory()).isEqualTo("IT");
        assertThat(response.getRefreshedCount()).isEqualTo(1);
        assertThat(mapper.savedCommands.get(0).getRecommendationScore()).isEqualTo(79);
    }

    @Test
    @DisplayName("fallback 결과가 POSSIBLE 30이어도 저장 직전 POSSIBLE 40으로 보정한다")
    void refreshPersonalRecommendations_normalizesFallbackPossibleScoreBeforeSaving() {
        FakeMapper mapper = new FakeMapper(List.of(
                ExternalJobDTO.builder()
                        .id(1L)
                        .title("사무 지원 담당자")
                        .jobCategoryRaw("업무지원")
                        .description("문서 정리")
                        .freelanceType(ExternalFreelanceType.CONTRACT_LIKE)
                        .recommendationType(ExternalJobRecommendationType.POSSIBLE)
                        .recommendationScore(30)
                        .build()));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, unavailableProvider());

        service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertThat(mapper.savedCommands.get(0).getRecommendationType())
                .isEqualTo(ExternalJobRecommendationType.POSSIBLE);
        assertThat(mapper.savedCommands.get(0).getRecommendationScore()).isEqualTo(40);
    }

    @Test
    @DisplayName("Gemini 결과도 저장 직전 추천 타입별 점수 구간으로 보정한다")
    void refreshPersonalRecommendations_normalizesGeminiScoreBeforeSaving() {
        FakeMapper mapper = new FakeMapper(List.of(
                candidate(1L, ExternalFreelanceType.PROJECT_LIKE, 77),
                candidate(2L, ExternalFreelanceType.PROJECT_LIKE, 77),
                candidate(3L, ExternalFreelanceType.PROJECT_LIKE, 77)));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, availableProvider(List.of(
                personalRecommendation(1L, ExternalJobRecommendationType.POSSIBLE, 30),
                personalRecommendation(2L, ExternalJobRecommendationType.RECOMMENDED, 85),
                personalRecommendation(3L, ExternalJobRecommendationType.EXCLUDED, 70))));

        service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertThat(mapper.savedCommands)
                .extracting(ExternalJobUserRecommendationCommand::getRecommendationType,
                        ExternalJobUserRecommendationCommand::getRecommendationScore)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.POSSIBLE, 40),
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.RECOMMENDED, 79),
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.EXCLUDED, 39)
                );
    }

    @Test
    @DisplayName("IT 웹 개발자 공고는 EXCLUDED로 내려와도 hard negative가 없으면 POSSIBLE로 보정한다")
    void refreshPersonalRecommendations_promotesItWebDeveloperExcludedRecommendation() {
        FakeMapper mapper = new FakeMapper(List.of(
                itJob(1L, "웹 개발자 모집", "웹 개발", "프론트엔드 웹 서비스 개발")));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, availableProvider(List.of(
                personalRecommendation(1L, ExternalJobRecommendationType.EXCLUDED, 20))));

        service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertPossibleAtLeast40(mapper.savedCommands.get(0));
    }

    @Test
    @DisplayName("IT 백엔드 개발자 공고는 EXCLUDED로 내려와도 POSSIBLE 이상으로 보정한다")
    void refreshPersonalRecommendations_promotesItBackendDeveloperExcludedRecommendation() {
        FakeMapper mapper = new FakeMapper(List.of(
                itJob(1L, "백엔드 개발자 채용", "서버 개발", "Java Spring 기반 API 서버 개발")));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, availableProvider(List.of(
                personalRecommendation(1L, ExternalJobRecommendationType.EXCLUDED, 30))));

        service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertPossibleAtLeast40(mapper.savedCommands.get(0));
    }

    @Test
    @DisplayName("IT 데이터/DB/클라우드 공고는 EXCLUDED로 내려와도 POSSIBLE 이상으로 보정한다")
    void refreshPersonalRecommendations_promotesItDataDbCloudExcludedRecommendation() {
        FakeMapper mapper = new FakeMapper(List.of(
                itJob(1L, "데이터 엔지니어", "DB 관리", "Python 데이터 파이프라인 개발"),
                itJob(2L, "DB 운영 담당자", "데이터베이스", "DB 성능 개선"),
                itJob(3L, "클라우드 서버 엔지니어", "클라우드", "서버 인프라 운영")));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, availableProvider(List.of(
                personalRecommendation(1L, ExternalJobRecommendationType.EXCLUDED, 10),
                personalRecommendation(2L, ExternalJobRecommendationType.EXCLUDED, 20),
                personalRecommendation(3L, ExternalJobRecommendationType.EXCLUDED, 30))));

        service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertThat(mapper.savedCommands).allSatisfy(ExternalJobRecommendationServiceTest::assertPossibleAtLeast40);
    }

    @Test
    @DisplayName("IT positive가 있어도 hard negative 사무보조/경리/주차관리 등은 EXCLUDED를 유지한다")
    void refreshPersonalRecommendations_keepsHardNegativeItJobsExcluded() {
        FakeMapper mapper = new FakeMapper(List.of(
                itJob(1L, "IT 회사 사무보조", "사무보조", "문서 정리"),
                itJob(2L, "IT 부서 경리", "경리", "회계 처리"),
                itJob(3L, "IT 주차관리 운전 생산 담당자", "주차관리", "운전 및 생산 지원")));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, availableProvider(List.of(
                personalRecommendation(1L, ExternalJobRecommendationType.EXCLUDED, 70),
                personalRecommendation(2L, ExternalJobRecommendationType.EXCLUDED, 70),
                personalRecommendation(3L, ExternalJobRecommendationType.EXCLUDED, 70))));

        service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertThat(mapper.savedCommands)
                .extracting(ExternalJobUserRecommendationCommand::getRecommendationType,
                        ExternalJobUserRecommendationCommand::getRecommendationScore)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.EXCLUDED, 39),
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.EXCLUDED, 39),
                        org.assertj.core.api.Assertions.tuple(ExternalJobRecommendationType.EXCLUDED, 39)
                );
    }

    @Test
    @DisplayName("fallback도 IT 후보 EXCLUDED 결과를 저장 직전 POSSIBLE로 보정한다")
    void refreshPersonalRecommendations_promotesFallbackItExcludedRecommendation() {
        FakeMapper mapper = new FakeMapper(List.of(
                ExternalJobDTO.builder()
                        .id(1L)
                        .title("소프트웨어 개발자 모집")
                        .jobCategoryRaw("소프트웨어")
                        .description("React 웹 앱 개발")
                        .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                        .recommendationType(ExternalJobRecommendationType.EXCLUDED)
                        .recommendationScore(20)
                        .build()));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, unavailableProvider());

        service.refreshPersonalRecommendations("user@lancit.com", "IT");

        assertPossibleAtLeast40(mapper.savedCommands.get(0));
    }

    @Test
    @DisplayName("refresh는 PROJECT_LIKE/CONTRACT_LIKE/TRUE_FREELANCE 후보 전체를 유저별 추천으로 저장한다")
    void refreshPersonalRecommendations_savesEveryVisibleFreelanceCandidate() {
        FakeMapper mapper = new FakeMapper(List.of(
                candidate(1L, ExternalFreelanceType.PROJECT_LIKE, 77),
                candidate(2L, ExternalFreelanceType.CONTRACT_LIKE, 41),
                candidate(3L, ExternalFreelanceType.TRUE_FREELANCE, 88),
                candidate(4L, ExternalFreelanceType.CONTRACT_LIKE, 35),
                candidate(5L, ExternalFreelanceType.PROJECT_LIKE, 70)));
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(mapper, unavailableProvider());

        ExternalJobRecommendationRefreshResponse response =
                service.refreshPersonalRecommendations("user@lancit.com", "업무지원");

        assertThat(response.getRefreshedCount()).isEqualTo(5);
        assertThat(mapper.savedCommands)
                .extracting(ExternalJobUserRecommendationCommand::getExternalJobId)
                .containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("refresh 요청 직종이 비어 있으면 INVALID_INPUT을 반환한다")
    void refreshPersonalRecommendations_rejectsBlankJobCategory() {
        ObjectProvider<GeminiExternalJobPersonalRecommendationClient> provider = unavailableProvider();
        ExternalJobRecommendationService service = new ExternalJobRecommendationService(new FakeMapper(List.of()), provider);

        assertThatThrownBy(() -> service.refreshPersonalRecommendations("user@lancit.com", " "))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private static ExternalJobDTO candidate(Long id, ExternalFreelanceType freelanceType, int score) {
        return ExternalJobDTO.builder()
                .id(id)
                .title("서울시 업무지원 공고 " + id)
                .jobCategoryRaw("업무지원")
                .description("업무지원")
                .freelanceType(freelanceType)
                .recommendationType(score >= 65
                        ? ExternalJobRecommendationType.RECOMMENDED
                        : ExternalJobRecommendationType.POSSIBLE)
                .recommendationScore(score)
                .build();
    }

    private static ExternalJobDTO itJob(Long id, String title, String jobCategoryRaw, String description) {
        return ExternalJobDTO.builder()
                .id(id)
                .title(title)
                .jobCategoryRaw(jobCategoryRaw)
                .description(description)
                .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                .recommendationType(ExternalJobRecommendationType.POSSIBLE)
                .recommendationScore(40)
                .build();
    }

    private static void assertPossibleAtLeast40(ExternalJobUserRecommendationCommand command) {
        assertThat(command.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
        assertThat(command.getRecommendationScore()).isGreaterThanOrEqualTo(40);
    }

    private static com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation personalRecommendation(
            Long externalJobId,
            ExternalJobRecommendationType recommendationType,
            int recommendationScore) {
        return com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation.builder()
                .externalJobId(externalJobId)
                .recommendationType(recommendationType)
                .recommendationScore(recommendationScore)
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
            List<com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation> recommendations) {
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
        private final List<com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation> recommendations;

        private StubGeminiClient(
                List<com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation> recommendations) {
            super(new ObjectMapper(), null);
            this.recommendations = recommendations;
        }

        @Override
        public List<com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation> recommend(
                String userJobCategory,
                List<ExternalJobDTO> jobs) {
            return recommendations;
        }
    }

    private static class FakeMapper implements ExternalJobMapper {
        private final List<ExternalJobDTO> jobs;
        private final List<ExternalJobUserRecommendationCommand> savedCommands = new ArrayList<>();

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
            savedCommands.add(command);
            return 1;
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
