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
        assertThat(result.getRecommendationScore()).isGreaterThanOrEqualTo(88);
    }

    @Test
    @DisplayName("디자인/개발/영상/콘텐츠/마케팅/글쓰기/교육 키워드는 프로젝트형 또는 추천으로 분류한다")
    void classify_projectKeyword() {
        ExternalJobClassification result = classify("콘텐츠 마케팅 영상 편집 프로젝트");

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.PROJECT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.RECOMMENDED);
        assertThat(result.getRecommendationScore()).isEqualTo(77);
    }

    @Test
    @DisplayName("IT컨설턴트/시스템컨설팅/IT프로젝트 PM 공고는 최상위 프로젝트형 추천으로 분류한다")
    void classify_itConsultantProject_highlyRecommended() {
        ExternalJobClassification result = classifier.classify(ExternalJobClassificationInput.builder()
                .title("IT컨설턴트 구인")
                .jobCategoryRaw("컴퓨터시스템 설계 및 분석가")
                .description("IT컨설턴트, ERP컨설턴트, 시스템컨설팅, IT프로젝트 PM")
                .employmentTypeRaw("주간")
                .salaryRaw("최소연봉 / 2600만원")
                .build());

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.PROJECT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.HIGHLY_RECOMMENDED);
        assertThat(result.getRecommendationScore()).isGreaterThanOrEqualTo(90);
    }

    @Test
    @DisplayName("IT 키워드가 있어도 hard negative 사무보조/경리는 제외한다")
    void classify_itHardNegative_excluded() {
        ExternalJobClassification result = classifier.classify(ExternalJobClassificationInput.builder()
                .title("IT 사무보조 구인")
                .description("사무보조, 경리, 문서정리")
                .build());

        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.EXCLUDED);
        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.NOT_FREELANCE);
    }

    @Test
    @DisplayName("계약직/기간제 키워드는 계약형 또는 검토 가능으로 분류한다")
    void classify_contractKeyword() {
        ExternalJobClassification result = classify("기간제 사무 보조 계약직 모집");

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.CONTRACT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
        assertThat(result.getRecommendationScore()).isEqualTo(41);
    }

    @Test
    @DisplayName("명확한 정규직 신호가 없는 애매한 서울시 공고는 제외하지 않고 계약형으로 둔다")
    void classify_ambiguousJob_contractLike() {
        ExternalJobClassification result = classify("민원 응대 업무지원 담당자 모집");

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.CONTRACT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
    }

    @Test
    @DisplayName("정규직/상용직/풀타임 일반 채용은 제외한다")
    void classify_negativeKeyword() {
        ExternalJobClassification result = classify("정규직 상용직 풀타임 일반 사무직 모집");

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.NOT_FREELANCE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.EXCLUDED);
        assertThat(result.getRecommendationScore()).isZero();
    }

    @Test
    @DisplayName("계약직 청소원 공고는 직무군 때문에 제외하지 않고 계약형으로 분류한다")
    void classify_cleaningContractJob_contractLike() {
        ExternalJobClassification result = classifier.classify(ExternalJobClassificationInput.builder()
                .title("[매탄동] 아파트 청소원 구인")
                .jobCategoryRaw("건물 청소원")
                .description("위생관리 용역, 근로자 파견")
                .employmentTypeRaw("계약직")
                .build());

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.CONTRACT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
    }

    @Test
    @DisplayName("계약형 물류 공고는 노출 후보로 두고 상용직 주차 공고는 제외한다")
    void classify_contractLogisticsVisible_permanentParkingExcluded() {
        ExternalJobClassification logistics = classifier.classify(ExternalJobClassificationInput.builder()
                .title("[쿠팡 인천45센터 대규모 채용] 물류센터 근무")
                .companyName("쿠팡풀필먼트서비스 유한회사")
                .description("전자상거래업, 소프트웨어 개발 및 공급업, 물류대행서비스업")
                .employmentTypeRaw("계약직")
                .build());
        ExternalJobClassification parking = classifier.classify(ExternalJobClassificationInput.builder()
                .title("주차관리요원")
                .companyName("엘슨개발")
                .description("부동산 임대 사업")
                .employmentTypeRaw("상용직")
                .build());

        assertThat(logistics.getFreelanceType()).isEqualTo(ExternalFreelanceType.CONTRACT_LIKE);
        assertThat(logistics.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
        assertThat(parking.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.EXCLUDED);
    }

    @Test
    @DisplayName("회사명에 개발이 있어도 실제 직무 근거가 없으면 프로젝트형으로 올리지 않는다")
    void classify_companyNameDevelopmentOnly_possible() {
        ExternalJobClassification result = classifier.classify(ExternalJobClassificationInput.builder()
                .title("총무 사무 보조 모집")
                .companyName("랜싯개발")
                .description("문서 정리 및 사무 보조")
                .employmentTypeRaw("계약직")
                .build());

        assertThat(result.getFreelanceType()).isEqualTo(ExternalFreelanceType.CONTRACT_LIKE);
        assertThat(result.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
    }

    @Test
    @DisplayName("기간제/계약직 생산·건설 업무도 계약형 후보로 분류한다")
    void classify_productionAndConstructionContracts_contractLike() {
        ExternalJobClassification production = classifier.classify(ExternalJobClassificationInput.builder()
                .title("K뷰티 화장품제조업체 산업기능요원 보충역 모집")
                .jobCategoryRaw("화장품·비누제품 생산기계 조작원")
                .description("화장품 생산 보조")
                .employmentTypeRaw("기간제")
                .build());
        ExternalJobClassification construction = classifier.classify(ExternalJobClassificationInput.builder()
                .title("건설현장 사무보조원 모집")
                .description("지반조사 및 건설현장 업무 보조")
                .employmentTypeRaw("계약직")
                .build());

        assertThat(production.getFreelanceType()).isEqualTo(ExternalFreelanceType.CONTRACT_LIKE);
        assertThat(construction.getFreelanceType()).isEqualTo(ExternalFreelanceType.CONTRACT_LIKE);
        assertThat(production.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
        assertThat(construction.getRecommendationType()).isEqualTo(ExternalJobRecommendationType.POSSIBLE);
    }

    private ExternalJobClassification classify(String title) {
        return classifier.classify(ExternalJobClassificationInput.builder()
                .title(title)
                .build());
    }
}
