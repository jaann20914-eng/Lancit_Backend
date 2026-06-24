package com.ssafy.lancit.domain.externaljob;

import com.ssafy.lancit.domain.externaljob.classifier.RuleBasedExternalJobClassifier;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedExternalJobClassifierTest {

    private final RuleBasedExternalJobClassifier classifier = new RuleBasedExternalJobClassifier();

    @Test
    @DisplayName("프리랜서/외주 키워드는 프리랜서형으로 분류한다")
    void classify_freelanceKeyword() {
        ExternalJobClassification result = classify("프리랜서 외주 웹 개발자를 찾습니다");

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.TRUE_FREELANCE);
        assertThat(result.getRecommendationType()).isIn(
                ExternalJobRecommendationType.HIGHLY_RECOMMENDED,
                ExternalJobRecommendationType.RECOMMENDED);
    }

    @Test
    @DisplayName("디자인/개발/영상/콘텐츠/마케팅/글쓰기/교육 키워드는 프로젝트형 또는 추천으로 분류한다")
    void classify_projectKeyword() {
        ExternalJobClassification result = classify("콘텐츠 마케팅 영상 편집 프로젝트");

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.PROJECT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.RECOMMENDED);
    }

    @Test
    @DisplayName("계약직/기간제 키워드는 계약형 또는 검토 가능으로 분류한다")
    void classify_contractKeyword() {
        ExternalJobClassification result = classify("기간제 사무 보조 계약직 모집");

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.CONTRACT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
    }

    @Test
    @DisplayName("정규직/상근/생산직/배송/주방/경비/미화류는 제외한다")
    void classify_negativeKeyword() {
        ExternalJobClassification result = classify("정규직 상근 생산직 배송 담당자 모집");

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.NOT_FREELANCE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.EXCLUDED);
    }

    private ExternalJobClassification classify(String title) {
        return classifier.classify(ExternalJobClassificationInput.builder()
                .title(title)
                .build());
    }
}
