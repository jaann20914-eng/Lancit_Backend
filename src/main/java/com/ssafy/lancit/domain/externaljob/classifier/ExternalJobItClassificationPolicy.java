package com.ssafy.lancit.domain.externaljob.classifier;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import org.springframework.util.StringUtils;

import java.util.List;

final class ExternalJobItClassificationPolicy {

    private static final List<String> SPECIFIC_IT_POSITIVE_KEYWORDS = List.of(
            "it컨설턴트", "erp컨설턴트", "시스템컨설팅", "it프로젝트", "it프로젝트 pm",
            "프로젝트 pm", "컴퓨터시스템 설계", "컴퓨터시스템 분석", "시스템 설계",
            "시스템 분석가"
    );

    private static final List<String> GENERIC_IT_POSITIVE_KEYWORDS = List.of(
            "it", "소프트웨어", "개발", "웹", "앱", "서버",
            "데이터", "보안", "네트워크", "인프라", "클라우드"
    );

    private static final List<String> HARD_NEGATIVE_KEYWORDS = List.of(
            "사무보조", "사무 보조", "경리", "회계", "주차", "주차관리", "운전",
            "배송", "생산", "제조", "청소", "미화", "조리", "요양", "간병",
            "콜센터", "텔레마케팅", "매장관리", "판매", "단순노무"
    );

    private ExternalJobItClassificationPolicy() {
    }

    static ExternalJobClassification classifyStrongItCandidate(ExternalJobClassificationInput input) {
        if (!hasStrongItPositiveSignal(input)) {
            return null;
        }
        if (hasHardNegativeSignal(input)) {
            return normalize(ExternalJobClassification.builder()
                    .freelanceType(ExternalFreelanceType.NOT_FREELANCE)
                    .recommendationType(ExternalJobRecommendationType.EXCLUDED)
                    .recommendationScore(0)
                    .label("외부 공고 탭 제외")
                    .confidence(0.88)
                    .reason("IT 키워드가 있으나 hard negative 직무 키워드 감지")
                    .build());
        }
        return normalize(strongItPositiveClassification());
    }

    static ExternalJobClassification applyStrongItOverride(ExternalJobClassificationInput input,
                                                           ExternalJobClassification classification) {
        if (!hasStrongItPositiveSignal(input)) {
            return normalize(classification);
        }
        if (hasHardNegativeSignal(input)) {
            return classifyStrongItCandidate(input);
        }
        return normalize(strongItPositiveClassification());
    }

    private static ExternalJobClassification strongItPositiveClassification() {
        return ExternalJobClassification.builder()
                .freelanceType(ExternalFreelanceType.PROJECT_LIKE)
                .recommendationType(ExternalJobRecommendationType.HIGHLY_RECOMMENDED)
                .recommendationScore(99)
                .label("IT 프로젝트형 공고")
                .confidence(0.95)
                .reason("강한 IT/개발/프로젝트 직무 키워드 감지")
                .build();
    }

    private static boolean hasStrongItPositiveSignal(ExternalJobClassificationInput input) {
        return containsAny(classificationText(input), SPECIFIC_IT_POSITIVE_KEYWORDS)
                || containsAny(titleAndCategoryText(input), GENERIC_IT_POSITIVE_KEYWORDS);
    }

    private static boolean hasHardNegativeSignal(ExternalJobClassificationInput input) {
        return containsAny(classificationText(input), HARD_NEGATIVE_KEYWORDS);
    }

    private static boolean containsAny(String text, List<String> keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return keywords.stream()
                .map(ExternalJobItClassificationPolicy::normalizeText)
                .anyMatch(text::contains);
    }

    private static String classificationText(ExternalJobClassificationInput input) {
        if (input == null) {
            return "";
        }
        return normalizeText(String.join(" ",
                value(input.getTitle()),
                value(input.getDescription()),
                value(input.getJobCategoryRaw()),
                value(input.getEmploymentTypeRaw())
        ));
    }

    private static String titleAndCategoryText(ExternalJobClassificationInput input) {
        if (input == null) {
            return "";
        }
        return normalizeText(String.join(" ",
                value(input.getTitle()),
                value(input.getJobCategoryRaw())
        ));
    }

    private static ExternalJobClassification normalize(ExternalJobClassification classification) {
        return ExternalJobClassificationPolicy.normalize(classification);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
