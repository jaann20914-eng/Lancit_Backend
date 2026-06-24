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
            List.of("프리랜서", "외주", "위탁", "용역", "재택", "원격");
    private static final List<String> PROJECT_KEYWORDS = List.of(
            "프로젝트", "디자인", "개발", "웹", "앱", "영상", "콘텐츠", "마케팅",
            "글쓰기", "작가", "강사", "교육", "음악", "편집", "번역", "기획"
    );
    private static final List<String> CONTRACT_KEYWORDS =
            List.of("계약직", "기간제", "단기", "파트타임", "시간제");
    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "정규직", "상근", "교대", "생산직", "운전", "배송", "주방", "홀서빙",
            "요양보호", "경비", "미화", "간호조무", "콜센터 상주"
    );

    @Override
    public ExternalJobClassification classify(ExternalJobClassificationInput input) {
        String text = combinedText(input);

        if (containsAny(text, NEGATIVE_KEYWORDS)) {
            return classification(
                    ExternalFreelanceType.NOT_FREELANCE,
                    ExternalJobRecommendationType.EXCLUDED,
                    "외부 공고 탭 제외",
                    0.9,
                    "부적합 키워드 감지");
        }

        if (containsAny(text, STRONG_FREELANCE_KEYWORDS)) {
            return classification(
                    ExternalFreelanceType.TRUE_FREELANCE,
                    text.contains("프리랜서") || text.contains("외주")
                            ? ExternalJobRecommendationType.HIGHLY_RECOMMENDED
                            : ExternalJobRecommendationType.RECOMMENDED,
                    "프리랜서형 공고",
                    0.82,
                    "프리랜서/외주성 키워드 감지");
        }

        if (containsAny(text, PROJECT_KEYWORDS)) {
            return classification(
                    ExternalFreelanceType.PROJECT_LIKE,
                    ExternalJobRecommendationType.RECOMMENDED,
                    "프로젝트형 공고",
                    0.72,
                    "프로젝트/결과물 중심 키워드 감지");
        }

        if (containsAny(text, CONTRACT_KEYWORDS)) {
            return classification(
                    ExternalFreelanceType.CONTRACT_LIKE,
                    ExternalJobRecommendationType.POSSIBLE,
                    "계약형 공고",
                    0.68,
                    "계약/기간제 키워드 감지");
        }

        return classification(
                ExternalFreelanceType.UNKNOWN,
                ExternalJobRecommendationType.POSSIBLE,
                "검토 가능",
                0.45,
                "명확한 제외 사유 없음");
    }

    private static ExternalJobClassification classification(ExternalFreelanceType freelanceType,
                                                            ExternalJobRecommendationType recommendationType,
                                                            String label,
                                                            double confidence,
                                                            String reason) {
        return ExternalJobClassification.builder()
                .freelanceType(freelanceType)
                .recommendationType(recommendationType)
                .label(label)
                .confidence(confidence)
                .reason(reason)
                .build();
    }

    private static boolean containsAny(String text, List<String> keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return keywords.stream().anyMatch(text::contains);
    }

    private static String combinedText(ExternalJobClassificationInput input) {
        if (input == null) {
            return "";
        }
        return String.join(" ",
                value(input.getTitle()),
                value(input.getCompanyName()),
                value(input.getDescription()),
                value(input.getJobCategoryRaw()),
                value(input.getEmploymentTypeRaw()),
                value(input.getLocation()),
                value(input.getSalaryRaw())
        ).toLowerCase();
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
