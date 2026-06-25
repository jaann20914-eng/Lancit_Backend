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
            "글쓰기", "작가", "강사", "교육", "음악", "편집", "번역", "기획",
            "pm", "컨설팅", "운영 대행", "제작"
    );
    private static final List<String> CONTRACT_KEYWORDS = List.of(
            "계약직", "기간제", "단기", "파트타임", "시간제", "비상근", "위촉",
            "촉탁", "임시직", "일용직", "아르바이트", "알바", "업무지원",
            "업무 지원", "지원 업무", "사무보조", "사무 보조", "보조원",
            "대체인력", "대체 인력", "시급", "시간 선택제"
    );
    private static final List<String> PERMANENT_EMPLOYMENT_KEYWORDS = List.of(
            "정규직", "상용직", "무기계약", "풀타임", "전일제", "상근"
    );

    @Override
    public ExternalJobClassification classify(ExternalJobClassificationInput input) {
        String exclusionText = exclusionText(input);
        String recommendationText = recommendationText(input);

        boolean hasContractSignal = containsAny(recommendationText, CONTRACT_KEYWORDS);

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

        ExternalJobClassification strongItClassification =
                ExternalJobItClassificationPolicy.classifyStrongItCandidate(input);
        if (strongItClassification != null) {
            return strongItClassification;
        }

        if (!hasContractSignal && containsAny(exclusionText, PERMANENT_EMPLOYMENT_KEYWORDS)) {
            return classification(
                    ExternalFreelanceType.NOT_FREELANCE,
                    ExternalJobRecommendationType.EXCLUDED,
                    0,
                    "외부 공고 탭 제외",
                    0.9,
                    "정규직/상용직 등 명확한 일반 채용 신호 감지");
        }

        if (hasContractSignal) {
            return classification(
                    ExternalFreelanceType.CONTRACT_LIKE,
                    ExternalJobRecommendationType.POSSIBLE,
                    41,
                    "계약형 공고",
                    0.68,
                    "계약/기간제 키워드 감지");
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

        return classification(
                ExternalFreelanceType.CONTRACT_LIKE,
                ExternalJobRecommendationType.POSSIBLE,
                41,
                "계약 가능성 공고",
                0.45,
                "명확한 정규직/상용직 제외 사유 없음");
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
