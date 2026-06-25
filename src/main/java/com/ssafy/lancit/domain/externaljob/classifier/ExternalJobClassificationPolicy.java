package com.ssafy.lancit.domain.externaljob.classifier;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;

public final class ExternalJobClassificationPolicy {

    private static final double DEFAULT_CONFIDENCE = 0.5;

    private ExternalJobClassificationPolicy() {
    }

    public static ExternalJobClassification normalize(ExternalJobClassification classification) {
        ExternalFreelanceType freelanceType = classification == null || classification.getFreelanceType() == null
                ? ExternalFreelanceType.UNKNOWN
                : classification.getFreelanceType();
        ExternalJobRecommendationType recommendationType =
                classification == null || classification.getRecommendationType() == null
                        ? defaultRecommendation(freelanceType)
                        : classification.getRecommendationType();

        if (ExternalFreelanceType.NOT_FREELANCE.equals(freelanceType)) {
            recommendationType = ExternalJobRecommendationType.EXCLUDED;
        }
        if (ExternalJobRecommendationType.EXCLUDED.equals(recommendationType)) {
            freelanceType = ExternalFreelanceType.NOT_FREELANCE;
        }

        double confidence = normalizeConfidence(classification == null ? null : classification.getConfidence());
        int recommendationScore = normalizeRecommendationScore(
                classification == null ? null : classification.getRecommendationScore(),
                freelanceType,
                recommendationType,
                confidence);

        return ExternalJobClassification.builder()
                .freelanceType(freelanceType)
                .recommendationType(recommendationType)
                .recommendationScore(recommendationScore)
                .label(classification == null ? null : classification.getLabel())
                .confidence(confidence)
                .reason(classification == null ? null : classification.getReason())
                .build();
    }

    public static ExternalJobRecommendationType defaultRecommendation(ExternalFreelanceType freelanceType) {
        ExternalFreelanceType safeFreelanceType = freelanceType == null
                ? ExternalFreelanceType.UNKNOWN
                : freelanceType;
        return switch (safeFreelanceType) {
            case TRUE_FREELANCE, PROJECT_LIKE -> ExternalJobRecommendationType.RECOMMENDED;
            case CONTRACT_LIKE, UNKNOWN -> ExternalJobRecommendationType.POSSIBLE;
            case NOT_FREELANCE -> ExternalJobRecommendationType.EXCLUDED;
        };
    }

    private static int normalizeRecommendationScore(Integer score,
                                                    ExternalFreelanceType freelanceType,
                                                    ExternalJobRecommendationType recommendationType,
                                                    double confidence) {
        if (ExternalFreelanceType.NOT_FREELANCE.equals(freelanceType)
                || ExternalJobRecommendationType.EXCLUDED.equals(recommendationType)) {
            return 0;
        }

        int candidate = score == null
                ? defaultScore(freelanceType, recommendationType, confidence)
                : clamp(score, 0, 100);
        return switch (recommendationType) {
            case HIGHLY_RECOMMENDED -> clamp(candidate, 90, 100);
            case RECOMMENDED -> clamp(candidate, 65, 89);
            case POSSIBLE -> clamp(candidate, 30, 64);
            case EXCLUDED -> 0;
        };
    }

    private static int defaultScore(ExternalFreelanceType freelanceType,
                                    ExternalJobRecommendationType recommendationType,
                                    double confidence) {
        return switch (recommendationType) {
            case HIGHLY_RECOMMENDED -> 95 + confidenceBonus(confidence, 5);
            case RECOMMENDED -> switch (freelanceType) {
                case TRUE_FREELANCE -> 88;
                case PROJECT_LIKE -> 77;
                case CONTRACT_LIKE, UNKNOWN -> 65;
                case NOT_FREELANCE -> 0;
            };
            case POSSIBLE -> switch (freelanceType) {
                case TRUE_FREELANCE, PROJECT_LIKE -> 60;
                case CONTRACT_LIKE -> 41;
                case UNKNOWN -> 30;
                case NOT_FREELANCE -> 0;
            };
            case EXCLUDED -> 0;
        };
    }

    private static int confidenceBonus(double confidence, int maxBonus) {
        return (int) Math.round(normalizeConfidence(confidence) * maxBonus);
    }

    private static double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return DEFAULT_CONFIDENCE;
        }
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
