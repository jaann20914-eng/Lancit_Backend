package com.ssafy.lancit.domain.externaljob.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiExternalJobPersonalRecommendationClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 12_000;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final String API_KEY_PROPERTY = "gemini.api.key";
    private static final String API_BASE_URL_PROPERTY = "gemini.api.base-url";
    private static final String MODEL_PROPERTY = "gemini.model";
    private static final String DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String DEFAULT_MODEL = "gemini-3.1-flash-lite";
    private static final String MATCHED_BY_GEMINI = "GEMINI";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Autowired
    public GeminiExternalJobPersonalRecommendationClient(ObjectMapper objectMapper, Environment environment) {
        this(createTimeoutRestClient(), objectMapper, environment);
    }

    GeminiExternalJobPersonalRecommendationClient(RestClient restClient,
                                                  ObjectMapper objectMapper,
                                                  Environment environment) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    public List<ExternalJobPersonalRecommendation> recommend(String userJobCategory, List<ExternalJobDTO> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        GeminiConfig config = getGeminiConfig();
        config.validate();

        try {
            String requestBody = objectMapper.writeValueAsString(createRequestBody(createPrompt(userJobCategory, jobs)));
            ResponseEntity<String> response = restClient.post()
                    .uri(config.generateContentUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", config.apiKey())
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                throw new IllegalStateException("Gemini external job recommendation returned empty response");
            }
            return parseRecommendations(extractGenerateContentText(response.getBody()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh external job recommendations with Gemini", e);
        }
    }

    private Map<String, Object> createRequestBody(String prompt) {
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

    private String createPrompt(String userJobCategory, List<ExternalJobDTO> jobs) {
        try {
            return """
                    LANCIT 외부 공고 탭에서 로그인 유저에게 보여줄 서울시 일자리플러스센터 공고 추천 점수를 계산하세요.
                    반드시 JSON 객체 하나만 반환하세요.

                    유저 직종: %s

                    기준:
                    - userJobCategory와 실제 직무/업무 내용이 가까울수록 높은 점수
                    - recommendationType 허용 값: HIGHLY_RECOMMENDED, RECOMMENDED, POSSIBLE, EXCLUDED
                    - HIGHLY_RECOMMENDED 점수는 80~100, RECOMMENDED는 60~79, POSSIBLE은 40~59, EXCLUDED는 0~39
                    - 점수는 0~100 정수
                    - 원본 공고 ID는 반드시 externalJobId로 그대로 반환
                    - 공고 목록에 없는 ID를 만들지 말 것
                    - 숫자 점수는 내부 정렬용이며 사용자에게 노출되지 않음

                    응답 형식:
                    {
                      "recommendations": [
                        {
                          "externalJobId": 1,
                          "recommendationType": "RECOMMENDED",
                          "recommendationScore": 75
                        }
                      ]
                    }

                    공고 목록:
                    %s
                    """.formatted(
                    value(userJobCategory),
                    objectMapper.writeValueAsString(toPromptJobs(jobs)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Gemini external job recommendation prompt", e);
        }
    }

    private List<Map<String, Object>> toPromptJobs(List<ExternalJobDTO> jobs) {
        return jobs.stream()
                .map(this::toPromptJob)
                .toList();
    }

    private Map<String, Object> toPromptJob(ExternalJobDTO job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("externalJobId", job.getId());
        payload.put("title", value(job.getTitle()));
        payload.put("companyName", value(job.getCompanyName()));
        payload.put("jobCategoryRaw", value(job.getJobCategoryRaw()));
        payload.put("employmentTypeRaw", value(job.getEmploymentTypeRaw()));
        payload.put("location", value(job.getLocation()));
        payload.put("salaryRaw", value(job.getSalaryRaw()));
        payload.put("globalRecommendationType", job.getRecommendationType() == null
                ? ""
                : job.getRecommendationType().name());
        payload.put("globalRecommendationScore", clamp(job.getRecommendationScore()));
        payload.put("description", abbreviate(value(job.getDescription()), DESCRIPTION_MAX_LENGTH));
        return payload;
    }

    private List<ExternalJobPersonalRecommendation> parseRecommendations(String aiText) throws Exception {
        JsonNode root = objectMapper.readTree(extractJson(aiText));
        JsonNode recommendationsNode = root.isArray() ? root : root.path("recommendations");
        if (!recommendationsNode.isArray()) {
            throw new IllegalStateException("Gemini recommendation response is not an array");
        }

        List<ExternalJobPersonalRecommendation> recommendations = new ArrayList<>();
        for (JsonNode node : recommendationsNode) {
            Long externalJobId = parseLong(node.path("externalJobId"));
            if (externalJobId == null) {
                continue;
            }

            Integer score = node.path("recommendationScore").isNumber()
                    ? clamp(node.path("recommendationScore").asInt())
                    : null;
            ExternalJobRecommendationType type = parseEnum(
                    ExternalJobRecommendationType.class,
                    node.path("recommendationType").asText(null),
                    null);
            if (type == null) {
                type = recommendationTypeForScore(score == null
                        ? defaultScore(ExternalJobRecommendationType.POSSIBLE)
                        : score);
            }
            int normalizedScore = normalizeRecommendationScore(score, type);

            recommendations.add(ExternalJobPersonalRecommendation.builder()
                    .externalJobId(externalJobId)
                    .recommendationType(type)
                    .recommendationScore(normalizedScore)
                    .matchedBy(MATCHED_BY_GEMINI)
                    .build());
        }
        return recommendations;
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
        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start = firstJsonStart(objectStart, arrayStart);
        if (start < 0) {
            throw new IllegalStateException("Gemini recommendation response is not JSON");
        }
        char open = trimmed.charAt(start);
        char close = open == '[' ? ']' : '}';
        int end = trimmed.lastIndexOf(close);
        if (end < start) {
            throw new IllegalStateException("Gemini recommendation response is incomplete JSON");
        }
        return trimmed.substring(start, end + 1);
    }

    private int firstJsonStart(int objectStart, int arrayStart) {
        if (objectStart < 0) {
            return arrayStart;
        }
        if (arrayStart < 0) {
            return objectStart;
        }
        return Math.min(objectStart, arrayStart);
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

    private static Long parseLong(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual() && StringUtils.hasText(node.asText())) {
            try {
                return Long.parseLong(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static ExternalJobRecommendationType recommendationTypeForScore(int score) {
        if (score >= 80) {
            return ExternalJobRecommendationType.HIGHLY_RECOMMENDED;
        }
        if (score >= 60) {
            return ExternalJobRecommendationType.RECOMMENDED;
        }
        if (score >= 40) {
            return ExternalJobRecommendationType.POSSIBLE;
        }
        return ExternalJobRecommendationType.EXCLUDED;
    }

    private static int defaultScore(ExternalJobRecommendationType type) {
        if (ExternalJobRecommendationType.HIGHLY_RECOMMENDED.equals(type)) {
            return 95;
        }
        if (ExternalJobRecommendationType.RECOMMENDED.equals(type)) {
            return 77;
        }
        if (ExternalJobRecommendationType.POSSIBLE.equals(type)) {
            return 45;
        }
        return 0;
    }

    private static int normalizeRecommendationScore(Integer score, ExternalJobRecommendationType type) {
        int candidate = score == null ? defaultScore(type) : clamp(score);
        return switch (type) {
            case HIGHLY_RECOMMENDED -> Math.max(80, candidate);
            case RECOMMENDED -> Math.max(60, Math.min(79, candidate));
            case POSSIBLE -> Math.max(40, Math.min(59, candidate));
            case EXCLUDED -> Math.min(39, candidate);
        };
    }

    private static int clamp(Integer score) {
        return score == null ? 0 : Math.max(0, Math.min(100, score));
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3).trim() + "...";
    }

    private static String value(String text) {
        return text == null ? "" : text;
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
