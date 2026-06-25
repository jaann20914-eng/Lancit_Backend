package com.ssafy.lancit.domain.externaljob;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectionLogCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDetailResponse;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobQueryService;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalJobQueryServiceTest {

    @Test
    @DisplayName("외부 공고 상세 조회는 기존 공고 상세 UI에 필요한 상세 필드를 보강한다")
    void getExternalJob_returnsDetailResponseForDetailPage() {
        LocalDateTime postedAt = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime deadlineAt = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime collectedAt = LocalDateTime.of(2026, 6, 2, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 3, 10, 0);
        ExternalJobQueryService externalJobQueryService = new ExternalJobQueryService(new FakeExternalJobMapper(
                ExternalJobDTO.builder()
                .id(7L)
                .source(ExternalJobSource.SEOUL)
                .sourceJobId("SEOUL-7")
                .sourceUrl(ExternalJobSource.SEOUL.getSiteUrl())
                .title("IT 프로젝트 PM")
                .companyName("서울시")
                .location("서울")
                .jobCategoryRaw("컴퓨터시스템 설계 및 분석가")
                .employmentTypeRaw("계약직")
                .salaryRaw("월 500만원")
                .postedAt(postedAt)
                .deadlineAt(deadlineAt)
                .description("공공기관 IT 프로젝트 PM 업무를 수행합니다.")
                .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                .recommendationType(ExternalJobRecommendationType.RECOMMENDED)
                .recommendationScore(77)
                .collectedAt(collectedAt)
                .updatedAt(updatedAt)
                .build()));

        ExternalJobDetailResponse response = externalJobQueryService.getExternalJob(7L);

        assertThat(response.getExternalJobId()).isEqualTo(7L);
        assertThat(response.getSourceLabel()).isEqualTo("서울시 일자리플러스센터");
        assertThat(response.getSummary()).isEqualTo("공공기관 IT 프로젝트 PM 업무를 수행합니다.");
        assertThat(response.getContent()).isEqualTo("공공기관 IT 프로젝트 PM 업무를 수행합니다.");
        assertThat(response.getDescription()).isEqualTo("공공기관 IT 프로젝트 PM 업무를 수행합니다.");
        assertThat(response.getRequirements()).isEqualTo("컴퓨터시스템 설계 및 분석가 · 계약직");
        assertThat(response.getWorkLocation()).isEqualTo("서울");
        assertThat(response.getSalaryText()).isEqualTo("월 500만원");
        assertThat(response.getRecruitmentStartAt()).isEqualTo(postedAt);
        assertThat(response.getRecruitmentEndAt()).isEqualTo(deadlineAt);
        assertThat(response.getCreatedAt()).isEqualTo(postedAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(response.getDetailButtonLabel()).isEqualTo("상세 보기");
        assertThat(response.getSourceButtonLabel()).isEqualTo("사이트에서 확인");
        assertThat(response.getCanApply()).isFalse();
        assertThat(response.getIsApplied()).isFalse();
        assertThat(response.getIsBookmarked()).isFalse();
        assertThat(response.getExternalNotice()).contains("원문 사이트");
    }

    @Test
    @DisplayName("외부 공고 상세 조회는 잘못된 ID를 거부한다")
    void getExternalJob_rejectsInvalidId() {
        ExternalJobQueryService externalJobQueryService = new ExternalJobQueryService(new FakeExternalJobMapper(null));

        assertThatThrownBy(() -> externalJobQueryService.getExternalJob(0L))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private static class FakeExternalJobMapper implements ExternalJobMapper {
        private final ExternalJobDTO detail;

        private FakeExternalJobMapper(ExternalJobDTO detail) {
            this.detail = detail;
        }

        @Override
        public int upsertExternalJob(com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand command) {
            return 0;
        }

        @Override
        public List<ExternalJobDTO> findExternalJobs(com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition condition,
                                                     PageRequest pageRequest) {
            return List.of();
        }

        @Override
        public long countExternalJobs(com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition condition) {
            return 0;
        }

        @Override
        public List<ExternalJobDTO> findVisibleExternalJobsForRecommendation() {
            return List.of();
        }

        @Override
        public int upsertExternalJobUserRecommendation(
                com.ssafy.lancit.domain.externaljob.dto.ExternalJobUserRecommendationCommand command) {
            return 0;
        }

        @Override
        public ExternalJobDTO findById(Long id) {
            return detail;
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
