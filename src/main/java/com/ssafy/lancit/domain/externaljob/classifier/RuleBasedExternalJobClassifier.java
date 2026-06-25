package com.ssafy.lancit.domain.externaljob.classifier;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class RuleBasedExternalJobClassifier implements ExternalJobClassifier {

    private static final List<String> STRONG_FREELANCE_KEYWORDS =
            List.of("프리랜서", "외주", "재택", "원격");
    private static final List<String> PROJECT_KEYWORDS = List.of(
            "프로젝트", "디자인", "개발", "웹", "앱", "영상", "콘텐츠", "마케팅",
            "글쓰기", "작가", "강사", "교육", "음악", "편집", "번역", "기획"
    );
    private static final List<String> CONTRACT_KEYWORDS =
            List.of("계약직", "기간제", "단기", "파트타임", "시간제");
    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "정규직", "상근", "교대", "생산직", "운전", "배송", "주방", "홀서빙",
            "요양보호", "요양원", "요양", "간병", "간병인", "경비", "미화", "청소",
            "청소원", "조리", "조리사", "구내식당", "주차", "주차관리", "물류",
            "입출고", "부품관리", "조립", "조립원", "간호조무", "콜센터 상주",
            "경리", "영업", "판매", "용접", "현장소장", "건설현장", "건설",
            "토공", "지반", "지질", "탐사", "시설관리", "생산 보조", "생산기계",
            "산업기능요원", "보충역", "쿠팡"
    );

    @Override
    public ExternalJobClassification classify(ExternalJobClassificationInput input) {
        String exclusionText = exclusionText(input);
        String recommendationText = recommendationText(input);

        if (containsAny(exclusionText, NEGATIVE_KEYWORDS)) {
            return classification(
                    ExternalFreelanceType.NOT_FREELANCE,
                    ExternalJobRecommendationType.EXCLUDED,
                    0,
                    "외부 공고 탭 제외",
                    0.9,
                    "부적합 키워드 감지");
        }

        if (containsAny(recommendationText, STRONG_FREELANCE_KEYWORDS)) {
            boolean explicitFreelance = recommendationText.contains("프리랜서")
                    || recommendationText.contains("외주");
            return classification(
                    ExternalFreelanceType.TRUE_FREELANCE,
                    explicitFreelance
                            ? ExternalJobRecommendationType.HIGHLY_RECOMMENDED
                            : ExternalJobRecommendationType.RECOMMENDED,
                    explicitFreelance ? 99 : 88,
                    "프리랜서형 공고",
                    0.82,
                    "프리랜서/외주성 키워드 감지");
        }

        if (containsAny(recommendationText, PROJECT_KEYWORDS)) {
            return classification(
                    ExternalFreelanceType.PROJECT_LIKE,
                    ExternalJobRecommendationType.RECOMMENDED,
                    77,
                    "프로젝트형 공고",
                    0.72,
                    "프로젝트/결과물 중심 키워드 감지");
        }

        if (containsAny(recommendationText, CONTRACT_KEYWORDS)) {
            return classification(
                    ExternalFreelanceType.CONTRACT_LIKE,
                    ExternalJobRecommendationType.POSSIBLE,
                    41,
                    "계약형 공고",
                    0.68,
                    "계약/기간제 키워드 감지");
        }

        return classification(
                ExternalFreelanceType.UNKNOWN,
                ExternalJobRecommendationType.POSSIBLE,
                30,
                "검토 가능",
                0.45,
                "명확한 제외 사유 없음");
    }

    private static ExternalJobClassification classification(ExternalFreelanceType freelanceType,
                                                            ExternalJobRecommendationType recommendationType,
                                                            int recommendationScore,
                                                            String label,
                                                            double confidence,
                                                            String reason) {
        return ExternalJobClassificationPolicy.normalize(ExternalJobClassification.builder()
                .freelanceType(freelanceType)
                .recommendationType(recommendationType)
                .recommendationScore(recommendationScore)
                .label(label)
                .confidence(confidence)
                .reason(reason)
                .build());
    }

    private static boolean containsAny(String text, List<String> keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return keywords.stream().anyMatch(text::contains);
    }

    private static String exclusionText(ExternalJobClassificationInput input) {
        if (input == null) {
            return "";
        }
        return String.join(" ",
                value(input.getTitle()),
                value(input.getCompanyName()),
                value(input.getDescription()),
                value(input.getJobCategoryRaw()),
                value(input.getEmploymentTypeRaw()),
                value(input.getLocation())
        ).toLowerCase();
    }

    private static String recommendationText(ExternalJobClassificationInput input) {
        if (input == null) {
            return "";
        }
        return String.join(" ",
                value(input.getTitle()),
                value(input.getDescription()),
                value(input.getJobCategoryRaw()),
                value(input.getEmploymentTypeRaw())
        ).toLowerCase();
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
