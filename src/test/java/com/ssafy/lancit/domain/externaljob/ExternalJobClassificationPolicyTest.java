package com.ssafy.lancit.domain.externaljob;

import com.ssafy.lancit.domain.externaljob.classifier.ExternalJobClassificationPolicy;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalJobClassificationPolicyTest {

    @Test
    @DisplayName("NOT_FREELANCE는 항상 EXCLUDED와 0점으로 정규화한다")
    void normalize_forcesNotFreelanceToExcludedAndZeroScore() {
        ExternalJobClassification result = ExternalJobClassificationPolicy.normalize(ExternalJobClassification.builder()
                .freelanceType(ExternalFreelanceType.NOT_FREELANCE)
                .recommendationType(ExternalJobRecommendationType.RECOMMENDED)
                .recommendationScore(99)
                .confidence(2.0)
                .build());

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.NOT_FREELANCE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.EXCLUDED);
        assertThat(result.getRecommendationScore()).isZero();
        assertThat(result.getConfidence()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("추천 타입별 점수 구간을 벗어난 Gemini 점수는 저장 전 보정한다")
    void normalize_clampsScoreByRecommendationType() {
        ExternalJobClassification possible = ExternalJobClassificationPolicy.normalize(ExternalJobClassification.builder()
                .freelanceType(ExternalFreelanceType.UNKNOWN)
                .recommendationType(ExternalJobRecommendationType.POSSIBLE)
                .recommendationScore(95)
                .build());
        ExternalJobClassification recommended = ExternalJobClassificationPolicy.normalize(ExternalJobClassification.builder()
                .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                .recommendationType(ExternalJobRecommendationType.RECOMMENDED)
                .recommendationScore(95)
                .build());

        assertThat(possible.getRecommendationScore()).isEqualTo(64);
        assertThat(recommended.getRecommendationScore()).isEqualTo(89);
    }

    @Test
    @DisplayName("분류 결과가 없어도 UNKNOWN/POSSIBLE 기본값과 30점으로 저장한다")
    void normalize_defaultsUnknownPossibleClassification() {
        ExternalJobClassification result = ExternalJobClassificationPolicy.normalize(null);

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.UNKNOWN);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
        assertThat(result.getRecommendationScore()).isEqualTo(30);
    }
}
