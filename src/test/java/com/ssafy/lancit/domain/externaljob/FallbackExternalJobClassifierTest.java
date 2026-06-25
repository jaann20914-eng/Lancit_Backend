package com.ssafy.lancit.domain.externaljob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.domain.externaljob.classifier.FallbackExternalJobClassifier;
import com.ssafy.lancit.domain.externaljob.classifier.GeminiExternalJobClassifier;
import com.ssafy.lancit.domain.externaljob.classifier.RuleBasedExternalJobClassifier;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackExternalJobClassifierTest {

    @Test
    @DisplayName("Gemini가 IT컨설턴트 공고를 EXCLUDED로 내려도 최종 전역 분류에서 최상위 추천으로 보정한다")
    void classify_overridesGeminiExcludedForStrongItProject() {
        FallbackExternalJobClassifier classifier = new FallbackExternalJobClassifier(
                availableProvider(ExternalJobClassification.builder()
                        .freelanceType(ExternalFreelanceType.NOT_FREELANCE)
                        .recommendationType(ExternalJobRecommendationType.EXCLUDED)
                        .recommendationScore(0)
                        .build()),
                new RuleBasedExternalJobClassifier());

        ExternalJobClassification result = classifier.classify(itConsultantInput());

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.PROJECT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.HIGHLY_RECOMMENDED);
        assertThat(result.getRecommendationScore()).isGreaterThanOrEqualTo(90);
    }

    @Test
    @DisplayName("Gemini 후처리에서도 IT hard negative는 EXCLUDED를 유지한다")
    void classify_keepsGeminiExcludedForItHardNegative() {
        FallbackExternalJobClassifier classifier = new FallbackExternalJobClassifier(
                availableProvider(ExternalJobClassification.builder()
                        .freelanceType(ExternalFreelanceType.NOT_FREELANCE)
                        .recommendationType(ExternalJobRecommendationType.EXCLUDED)
                        .recommendationScore(0)
                        .build()),
                new RuleBasedExternalJobClassifier());

        ExternalJobClassification result = classifier.classify(ExternalJobClassificationInput.builder()
                .title("IT 사무보조 구인")
                .description("사무보조, 경리, 문서정리")
                .build());

        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.EXCLUDED);
        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.NOT_FREELANCE);
    }

    private static ExternalJobClassificationInput itConsultantInput() {
        return ExternalJobClassificationInput.builder()
                .title("IT컨설턴트 구인")
                .jobCategoryRaw("컴퓨터시스템 설계 및 분석가")
                .description("IT컨설턴트, ERP컨설턴트, 시스템컨설팅, IT프로젝트 PM")
                .employmentTypeRaw("주간")
                .salaryRaw("최소연봉 / 2600만원")
                .build();
    }

    private static ObjectProvider<GeminiExternalJobClassifier> availableProvider(
            ExternalJobClassification classification) {
        GeminiExternalJobClassifier geminiClassifier = new StubGeminiClassifier(classification);
        return new ObjectProvider<>() {
            @Override
            public GeminiExternalJobClassifier getObject(Object... args) {
                return geminiClassifier;
            }

            @Override
            public GeminiExternalJobClassifier getIfAvailable() {
                return geminiClassifier;
            }

            @Override
            public GeminiExternalJobClassifier getIfUnique() {
                return geminiClassifier;
            }

            @Override
            public GeminiExternalJobClassifier getObject() {
                return geminiClassifier;
            }
        };
    }

    private static class StubGeminiClassifier extends GeminiExternalJobClassifier {
        private final ExternalJobClassification classification;

        private StubGeminiClassifier(ExternalJobClassification classification) {
            super(new ObjectMapper(), null);
            this.classification = classification;
        }

        @Override
        public ExternalJobClassification classify(ExternalJobClassificationInput input) {
            return classification;
        }
    }
}
