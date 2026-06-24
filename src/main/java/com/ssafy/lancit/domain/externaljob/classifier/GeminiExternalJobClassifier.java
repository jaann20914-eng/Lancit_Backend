package com.ssafy.lancit.domain.externaljob.classifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiExternalJobClassifier implements ExternalJobClassifier {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;
    private static final String API_KEY_PROPERTY = "gemini.api.key";
    private static final String API_BASE_URL_PROPERTY = "gemini.api.base-url";
    private static final String MODEL_PROPERTY = "gemini.model";
    private static final String DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String DEFAULT_MODEL = "gemini-3.1-flash-lite";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Autowired
    public GeminiExternalJobClassifier(ObjectMapper objectMapper, Environment environment) {
        this(createTimeoutRestClient(), objectMapper, environment);
    }

    GeminiExternalJobClassifier(RestClient restClient, ObjectMapper objectMapper, Environment environment) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    public ExternalJobClassification classify(ExternalJobClassificationInput input) {
        GeminiConfig config = getGeminiConfig();
        config.validate();

        try {
            String requestBody = objectMapper.writeValueAsString(createRequestBody(createPrompt(input)));
            ResponseEntity<String> response = restClient.post()
                    .uri(config.generateContentUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", config.apiKey())
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                throw new IllegalStateException("Gemini external job classifier returned empty response");
            }
            return parseClassification(extractGenerateContentText(response.getBody()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to classify external job with Gemini", e);
        }
    }

    Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );
    }

    private String createPrompt(ExternalJobClassificationInput input) {
        return """
                LANCIT 외부 공고 탭에 저장할 채용 공고를 분류하세요.
                반드시 JSON 객체 하나만 반환하세요.

                허용 값:
                - freelanceType: TRUE_FREELANCE, PROJECT_LIKE, CONTRACT_LIKE, NOT_FREELANCE, UNKNOWN
                - recommendationType: HIGHLY_RECOMMENDED, RECOMMENDED, POSSIBLE, EXCLUDED

                기준:
                - 실제 직무/계약 조건에 프리랜서 또는 외주 수행이 명확하면 TRUE_FREELANCE
                - 프로젝트 단위, 결과물 중심 디자인/개발/영상/콘텐츠/마케팅/글쓰기/교육/기획 업무는 PROJECT_LIKE
                - 재택/원격 가능성만 명확하면 TRUE_FREELANCE가 아니라 PROJECT_LIKE 또는 CONTRACT_LIKE 중 더 가까운 값
                - 계약직, 기간제, 단기 근무이나 프리랜서가 관심 가질 수 있으면 CONTRACT_LIKE
                - 정규직, 상근 필수, 교대근무, 생산/노무/영업점 상주 업무는 NOT_FREELANCE
                - 청소/미화/조리/주차/물류/배송/요양/간병/경비/조립/용접/영업/판매/경리/생산/건설현장 업무는 NOT_FREELANCE
                - 회사명, 사업요약, 발주 설명에만 "개발", "위탁", "용역", "소프트웨어"가 있으면 추천 근거로 쓰지 말 것
                - 실제 직무가 제외 업무이면 회사명/사업명과 무관하게 NOT_FREELANCE
                - freelanceType이 NOT_FREELANCE이거나 recommendationType이 EXCLUDED이면 recommendationScore는 0
                - HIGHLY_RECOMMENDED 점수는 90~100, RECOMMENDED는 65~89, POSSIBLE은 30~64, EXCLUDED는 0
                - 명확히 부적합하지 않으면 UNKNOWN이어도 recommendationType은 POSSIBLE 가능

                응답 형식:
                {
                  "freelanceType": "PROJECT_LIKE",
                  "recommendationType": "RECOMMENDED",
                  "recommendationScore": 75,
                  "label": "프로젝트형 공고",
                  "confidence": 0.0,
                  "reason": "내부 디버깅용 한 문장"
                }

                공고:
                제목: %s
                회사: %s
                직무/분류: %s
                고용형태: %s
                근무지: %s
                급여: %s
                설명: %s
                """.formatted(
                value(input == null ? null : input.getTitle()),
                value(input == null ? null : input.getCompanyName()),
                value(input == null ? null : input.getJobCategoryRaw()),
                value(input == null ? null : input.getEmploymentTypeRaw()),
                value(input == null ? null : input.getLocation()),
                value(input == null ? null : input.getSalaryRaw()),
                value(input == null ? null : input.getDescription())
        );
    }

    private ExternalJobClassification parseClassification(String aiText) throws Exception {
        JsonNode root = objectMapper.readTree(extractJson(aiText));
        ExternalFreelanceType freelanceType = parseEnum(
                ExternalFreelanceType.class,
                root.path("freelanceType").asText(null),
                ExternalFreelanceType.UNKNOWN);
        ExternalJobRecommendationType recommendationType = parseEnum(
                ExternalJobRecommendationType.class,
                root.path("recommendationType").asText(null),
                ExternalJobClassificationPolicy.defaultRecommendation(freelanceType));
        double confidence = root.path("confidence").isNumber()
                ? Math.max(0.0, Math.min(1.0, root.path("confidence").asDouble()))
                : 0.5;
        Integer recommendationScore = root.path("recommendationScore").isNumber()
                ? Math.max(0, Math.min(100, root.path("recommendationScore").asInt()))
                : null;

        return ExternalJobClassificationPolicy.normalize(ExternalJobClassification.builder()
                .freelanceType(freelanceType)
                .recommendationType(recommendationType)
                .recommendationScore(recommendationScore)
                .label(trimToNull(root.path("label").asText(null)))
                .confidence(confidence)
                .reason(trimToNull(root.path("reason").asText(null)))
                .build());
    }

    private String extractGenerateContentText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode partsNode = root.path("candidates").path(0).path("content").path("parts");
        if (!partsNode.isArray() || partsNode.isEmpty()) {
            throw new IllegalStateException("Gemini response text parts are empty");
        }
        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode partNode : partsNode) {
            String text = partNode.path("text").asText(null);
            if (!StringUtils.hasText(text)) {
                continue;
            }
            if (textBuilder.length() > 0) {
                textBuilder.append('\n');
            }
            textBuilder.append(text);
        }
        if (!StringUtils.hasText(textBuilder.toString())) {
            throw new IllegalStateException("Gemini response text is empty");
        }
        return textBuilder.toString();
    }

    private String extractJson(String aiText) {
        String trimmed = aiText == null ? "" : aiText.trim()
                .replaceFirst("^```(?:json|JSON)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalStateException("Gemini classification response is not a JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private GeminiConfig getGeminiConfig() {
        return new GeminiConfig(
                getProperty(API_KEY_PROPERTY, ""),
                getProperty(API_BASE_URL_PROPERTY, DEFAULT_API_BASE_URL),
                getProperty(MODEL_PROPERTY, DEFAULT_MODEL));
    }

    private String getProperty(String propertyName, String defaultValue) {
        if (environment == null) {
            return defaultValue;
        }
        try {
            String value = environment.getProperty(propertyName);
            return value == null ? defaultValue : value.trim();
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static RestClient createTimeoutRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MILLIS));
        requestFactory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MILLIS));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, T defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static boolean isConfigured(String value) {
        return StringUtils.hasText(value) && !isUnresolvedPlaceholder(value);
    }

    private static boolean isUnresolvedPlaceholder(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.startsWith("${") && trimmedValue.endsWith("}");
    }

    private record GeminiConfig(String apiKey, String apiBaseUrl, String model) {
        private String generateContentUrl() {
            String normalizedBaseUrl = apiBaseUrl.endsWith("/")
                    ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1)
                    : apiBaseUrl;
            String normalizedModel = model.startsWith("models/") ? model.substring("models/".length()) : model;
            return normalizedBaseUrl + "/" + normalizedModel + ":generateContent";
        }

        private void validate() {
            if (!isConfigured(apiKey)) {
                throw new IllegalStateException("Gemini API key is not configured");
            }
            if (!isConfigured(apiBaseUrl) || !isConfigured(model)) {
                throw new IllegalStateException("Gemini model configuration is not complete");
            }
        }
    }
}
