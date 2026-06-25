package com.ssafy.lancit.domain.externaljob;

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
        assertThat(mapper.savedCommands.get(0).getRecommendationScore()).isEqualTo(82);
        assertThat(mapper.savedCommands.get(1).getRecommendationScore()).isEqualTo(45);
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
        public int upsertExternalJobUserRecommendation(ExternalJobUserRecommendationCommand command) {
            savedCommands.add(command);
            return 1;
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
