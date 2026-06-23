package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.global.enums.DateTimePrecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiTaskParseClient implements AiTaskParseClient {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final String API_KEY_PROPERTY = "gemini.api.key";
    private static final String API_KEY_ENV_PROPERTY = "GEMINI_API_KEY";
    private static final String API_BASE_URL_PROPERTY = "gemini.api.base-url";
    private static final String MODEL_PROPERTY = "gemini.model";
    private static final String DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String DEFAULT_MODEL = "gemini-3.5-flash";
    private static final List<String> REQUIRED_RESPONSE_FIELDS = List.of(
            "sourceText",
            "categoryId",
            "title",
            "content",
            "memo",
            "startAt",
            "startDate",
            "startTime",
            "startText",
            "startPrecision",
            "endAt",
            "endDate",
            "endTime",
            "endText",
            "endPrecision",
            "status",
            "clientCompany",
            "budget",
            "budgetAmount",
            "depositAmount",
            "paidAmount",
            "balanceAmount",
            "contractAmount",
            "budgetText",
            "paidAt",
            "paidDate",
            "paidTime",
            "paidText",
            "paidPrecision",
            "confidence",
            "warnings"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final Clock clock;

    @Autowired
    public GeminiTaskParseClient(ObjectMapper objectMapper, Environment environment, Clock clock) {
        this(createTimeoutRestClient(), objectMapper, environment, clock);
    }

    GeminiTaskParseClient(ObjectMapper objectMapper, Environment environment) {
        this(createTimeoutRestClient(), objectMapper, environment, Clock.system(SEOUL_ZONE));
    }

    GeminiTaskParseClient(ObjectMapper objectMapper, Clock clock) {
        this(createTimeoutRestClient(), objectMapper, null, clock);
    }

    GeminiTaskParseClient(RestClient restClient, ObjectMapper objectMapper, Environment environment, Clock clock) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.clock = clock == null ? Clock.system(SEOUL_ZONE) : clock;
    }

    @Override
    public TaskParseResponseDTO parse(String sourceText) {
        GeminiConfig config = getGeminiConfig();

        try {
            config.validate();
            TaskParseResponseDTO responseDTO = requestAndParseWithRetry(config, sourceText);
            log.info("Gemini task parsing succeeded. model={}, baseUrlPresent={}, apiKeyPresent={}",
                    config.model(), config.baseUrlPresent(), config.apiKeyPresent());
            return responseDTO;
        } catch (RuntimeException e) {
            log.warn("Gemini task parsing failed. model={}, baseUrlPresent={}, apiKeyPresent={}",
                    config.model(), config.baseUrlPresent(), config.apiKeyPresent());
            throw e;
        } catch (Exception e) {
            log.warn("Gemini task parsing failed. model={}, baseUrlPresent={}, apiKeyPresent={}",
                    config.model(), config.baseUrlPresent(), config.apiKeyPresent());
            throw new IllegalStateException("Failed to parse Gemini response", e);
        }
    }

    private TaskParseResponseDTO requestAndParseWithRetry(GeminiConfig config, String sourceText) throws Exception {
        try {
            return requestAndParse(config, createPrompt(sourceText));
        } catch (AiResponseValidationException firstFailure) {
            try {
                return requestAndParse(config, createRetryPrompt(sourceText, firstFailure.getMessage()));
            } catch (AiResponseValidationException retryFailure) {
                throw new IllegalStateException("Failed to parse Gemini response after retry", retryFailure);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse Gemini retry response", e);
            }
        }
    }

    private TaskParseResponseDTO requestAndParse(GeminiConfig config, String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(createRequestBody(prompt));
        ResponseEntity<String> response = restClient.post()
                .uri(config.generateContentUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", config.apiKey())
                .body(requestBody)
                .retrieve()
                .toEntity(String.class);

        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            throw new IllegalStateException("Gemini returned an empty or non-success response");
        }

        String aiText = extractGenerateContentText(response.getBody());
        return parseTaskResponse(aiText);
    }

    Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", createTaskParseResponseSchema()
                )
        );
    }

    String createPrompt(String sourceText) {
        LocalDate today = LocalDate.now(clock);
        return """
                자연어 일정 문장을 TaskParseResponseDTO JSON으로 파싱하세요.
                기준 날짜: %s, 시간대: Asia/Seoul

                출력 규칙:
                - responseSchema를 반드시 준수합니다.
                - sourceText는 원문 그대로, categoryId는 항상 null, status 기본값은 "IN_PROGRESS"입니다.
                - 알 수 없는 값은 null, warnings가 없으면 []로 반환합니다.
                - content는 null이 아닌 한 문장 요약, confidence는 0.0~1.0 숫자입니다.

                날짜/시간 규칙:
                - start*=회의/작업/일정, end*=종료, paid*=지급/입금/정산 예정일 또는 기한입니다.
                - 일정일과 지급일이 함께 있으면 start*와 paid*로 분리합니다.
                - 지급/입금/정산 중심 문장은 paid*에만 반영하고 start*/end*를 채우지 않습니다.
                - 날짜+시간이 확정된 경우에만 *At(ISO-8601 LocalDateTime)을 채우고 *Date, *Time도 함께 채웁니다.
                - 날짜만 있으면 *At/*Time=null, *Date=ISO-8601 LocalDate이며 임의 시간을 만들지 않습니다.
                - 시간만 있으면 *At/*Date=null, *Time=ISO-8601 LocalTime이며 임의 날짜를 만들지 않습니다.
                - 정보가 없으면 관련 *At/*Date/*Time/*Text=null, *Precision="NONE"입니다.
                - *Text에는 원문 날짜/시간 표현을 보존하고 정밀도는 DATE_TIME/DATE_ONLY/TIME_ONLY/NONE과 일치시킵니다.
                - 상대 날짜는 기준 날짜로 계산하되, 모호하거나 원문에 없는 날짜/시간은 추정하지 않습니다.
                - 명시된 종료 시각 또는 시작 시각+기간이 있을 때만 end*를 계산합니다.

                의미/금액 규칙:
                - title은 날짜·시간·장소·금액을 뺀 핵심 명사구입니다.
                - 장소, 회의실, 링크, 준비물, 회의 방식과 부가 설명은 memo에만 넣습니다.
                - clientCompany에는 회사/거래처만 넣고 사람·학교·장소·회의실·카페·온라인 정보는 넣지 않습니다.
                - budgetAmount=전체 예산/견적, depositAmount=계약금/선금, paidAmount=실제 입금/지급액,
                  balanceAmount=잔금, contractAmount=계약 총액, budgetText=숫자로 확정할 수 없는 범위/비율 원문입니다.
                - 서로 다른 금액 의미는 각 필드로 분리합니다. budget은 하위 호환 대표 금액으로 전체 예산을 우선하고,
                  없으면 paidAmount/depositAmount/balanceAmount/contractAmount 중 문맥상 핵심 금액을 사용합니다.
                - 사용자가 말하지 않은 회사, 금액, 카테고리 등은 추정하지 않습니다.

                사용자 원문:
                %s
                """.formatted(today, sourceText);
    }

    private String createRetryPrompt(String sourceText, String failureReason) {
        return """
                이전 응답은 DTO 검증에 실패했습니다.
                responseSchema의 모든 필드를 포함한 JSON만 다시 반환하세요.
                content는 null이 아닌 문자열이어야 합니다.
                이전 응답 검증 실패 사유: %s

                %s
                """.formatted(failureReason, createPrompt(sourceText));
    }

    String extractGenerateContentText(String responseBody) throws AiResponseValidationException {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new AiResponseValidationException("Gemini response is not valid JSON", e);
        }

        JsonNode candidatesNode = root.path("candidates");
        if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
            throw new AiResponseValidationException("Gemini response candidates are empty");
        }

        JsonNode partsNode = candidatesNode.get(0).path("content").path("parts");
        if (!partsNode.isArray() || partsNode.isEmpty()) {
            throw new AiResponseValidationException("Gemini response text parts are empty");
        }

        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode partNode : partsNode) {
            JsonNode textNode = partNode.path("text");
            if (!textNode.isTextual() || !StringUtils.hasText(textNode.asString())) {
                continue;
            }
            if (textBuilder.length() > 0) {
                textBuilder.append("\n");
            }
            textBuilder.append(textNode.asString());
        }
        if (!StringUtils.hasText(textBuilder.toString())) {
            throw new AiResponseValidationException("Gemini response text is empty");
        }
        return textBuilder.toString();
    }

    private TaskParseResponseDTO parseTaskResponse(String aiText) throws AiResponseValidationException {
        String json = extractJson(aiText);
        validateJsonSchema(json);
        try {
            TaskParseResponseDTO responseDTO = objectMapper.readValue(json, TaskParseResponseDTO.class);
            validateParsedDto(responseDTO);
            return responseDTO;
        } catch (AiResponseValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiResponseValidationException("DTO conversion failed", e);
        }
    }

    private String extractJson(String aiText) throws AiResponseValidationException {
        String trimmedText = aiText.trim()
                .replaceFirst("^```(?:json|JSON)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();

        int jsonStartIndex = trimmedText.indexOf('{');
        int jsonEndIndex = trimmedText.lastIndexOf('}');
        if (jsonStartIndex < 0 || jsonEndIndex < jsonStartIndex) {
            throw new AiResponseValidationException("AI response is not a JSON object");
        }
        return trimmedText.substring(jsonStartIndex, jsonEndIndex + 1).trim();
    }

    @SuppressWarnings("unchecked")
    private void validateJsonSchema(String json) throws AiResponseValidationException {
        Map<String, Object> responseMap;
        try {
            Object parsedJson = objectMapper.readValue(json, Map.class);
            if (!(parsedJson instanceof Map<?, ?> parsedMap)) {
                throw new AiResponseValidationException("AI response JSON is not an object");
            }
            responseMap = (Map<String, Object>) parsedMap;
        } catch (AiResponseValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiResponseValidationException("AI response is not valid JSON", e);
        }

        List<String> missingFields = REQUIRED_RESPONSE_FIELDS.stream()
                .filter(field -> !responseMap.containsKey(field))
                .toList();
        if (!missingFields.isEmpty()) {
            throw new AiResponseValidationException("AI response is missing required fields: " + missingFields);
        }

        List<String> unknownFields = responseMap.keySet().stream()
                .map(String::valueOf)
                .filter(field -> !REQUIRED_RESPONSE_FIELDS.contains(field))
                .toList();
        if (!unknownFields.isEmpty()) {
            throw new AiResponseValidationException("AI response contains unknown fields: " + unknownFields);
        }

        if (responseMap.get("categoryId") != null) {
            throw new AiResponseValidationException("categoryId must be null");
        }
        if (!(responseMap.get("warnings") instanceof List<?>)) {
            throw new AiResponseValidationException("warnings must be an array");
        }
    }

    private void validateParsedDto(TaskParseResponseDTO responseDTO) throws AiResponseValidationException {
        if (responseDTO == null) {
            throw new AiResponseValidationException("AI task parse result is empty");
        }
        if (!StringUtils.hasText(responseDTO.getTitle())) {
            throw new AiResponseValidationException("AI task parse result has no title");
        }
        if (!StringUtils.hasText(responseDTO.getContent())) {
            throw new AiResponseValidationException("AI task parse result has no content");
        }
        if (responseDTO.getStatus() == null) {
            throw new AiResponseValidationException("AI task parse result has no status");
        }
        if (responseDTO.getCategoryId() != null) {
            throw new AiResponseValidationException("categoryId must be null");
        }
        if (responseDTO.getWarnings() == null) {
            throw new AiResponseValidationException("warnings must be an array");
        }
        validateDateTimePrecision(
                "start",
                responseDTO.getStartAt(),
                responseDTO.getStartDate(),
                responseDTO.getStartTime(),
                responseDTO.getStartText(),
                responseDTO.getStartPrecision()
        );
        validateDateTimePrecision(
                "end",
                responseDTO.getEndAt(),
                responseDTO.getEndDate(),
                responseDTO.getEndTime(),
                responseDTO.getEndText(),
                responseDTO.getEndPrecision()
        );
        validateDateTimePrecision(
                "paid",
                responseDTO.getPaidAt(),
                responseDTO.getPaidDate(),
                responseDTO.getPaidTime(),
                responseDTO.getPaidText(),
                responseDTO.getPaidPrecision()
        );
    }

    private void validateDateTimePrecision(String fieldName,
                                           Object at,
                                           Object date,
                                           Object time,
                                           String text,
                                           DateTimePrecision precision) throws AiResponseValidationException {
        if (precision == null) {
            throw new AiResponseValidationException(fieldName + "Precision is required");
        }
        switch (precision) {
            case NONE -> {
                if (at != null || date != null || time != null || StringUtils.hasText(text)) {
                    throw new AiResponseValidationException(fieldName + " precision NONE must not include date/time values");
                }
            }
            case DATE_ONLY -> {
                if (at != null || date == null || time != null || !StringUtils.hasText(text)) {
                    throw new AiResponseValidationException(fieldName + " DATE_ONLY must include only date and text");
                }
            }
            case TIME_ONLY -> {
                if (at != null || date != null || time == null || !StringUtils.hasText(text)) {
                    throw new AiResponseValidationException(fieldName + " TIME_ONLY must include only time and text");
                }
            }
            case DATE_TIME -> {
                if (at == null || date == null || time == null || !StringUtils.hasText(text)) {
                    throw new AiResponseValidationException(fieldName + " DATE_TIME must include date, time, datetime, and text");
                }
            }
        }
    }

    private Map<String, Object> createTaskParseResponseSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("sourceText", stringSchema());
        properties.put("categoryId", nullSchema());
        properties.put("title", nullableStringSchema());
        properties.put("content", stringSchema());
        properties.put("memo", nullableStringSchema());
        properties.put("startAt", nullableStringSchema());
        properties.put("startDate", nullableStringSchema());
        properties.put("startTime", nullableStringSchema());
        properties.put("startText", nullableStringSchema());
        properties.put("startPrecision", enumSchema("NONE", "DATE_ONLY", "TIME_ONLY", "DATE_TIME"));
        properties.put("endAt", nullableStringSchema());
        properties.put("endDate", nullableStringSchema());
        properties.put("endTime", nullableStringSchema());
        properties.put("endText", nullableStringSchema());
        properties.put("endPrecision", enumSchema("NONE", "DATE_ONLY", "TIME_ONLY", "DATE_TIME"));
        properties.put("status", enumSchema("IN_PROGRESS", "COMPLETED", "CANCELLED"));
        properties.put("clientCompany", nullableStringSchema());
        properties.put("budget", nullableIntegerSchema());
        properties.put("budgetAmount", nullableIntegerSchema());
        properties.put("depositAmount", nullableIntegerSchema());
        properties.put("paidAmount", nullableIntegerSchema());
        properties.put("balanceAmount", nullableIntegerSchema());
        properties.put("contractAmount", nullableIntegerSchema());
        properties.put("budgetText", nullableStringSchema());
        properties.put("paidAt", nullableStringSchema());
        properties.put("paidDate", nullableStringSchema());
        properties.put("paidTime", nullableStringSchema());
        properties.put("paidText", nullableStringSchema());
        properties.put("paidPrecision", enumSchema("NONE", "DATE_ONLY", "TIME_ONLY", "DATE_TIME"));
        properties.put("confidence", nullableNumberSchema());
        properties.put("warnings", arraySchema(stringSchema()));

        return objectSchema(properties, REQUIRED_RESPONSE_FIELDS);
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private Map<String, Object> stringSchema() {
        return Map.of("type", "STRING");
    }

    private Map<String, Object> integerSchema() {
        return Map.of("type", "INTEGER");
    }

    private Map<String, Object> numberSchema() {
        return Map.of("type", "NUMBER");
    }

    private Map<String, Object> nullSchema() {
        return Map.of("type", "NULL");
    }

    private Map<String, Object> nullableStringSchema() {
        return nullableSchema(stringSchema());
    }

    private Map<String, Object> nullableIntegerSchema() {
        return nullableSchema(integerSchema());
    }

    private Map<String, Object> nullableNumberSchema() {
        return nullableSchema(numberSchema());
    }

    private Map<String, Object> nullableSchema(Map<String, Object> valueSchema) {
        Map<String, Object> nullableSchema = new LinkedHashMap<>(valueSchema);
        nullableSchema.put("nullable", true);
        return nullableSchema;
    }

    private Map<String, Object> enumSchema(String... values) {
        return Map.of(
                "type", "STRING",
                "enum", List.of(values)
        );
    }

    private Map<String, Object> arraySchema(Map<String, Object> itemSchema) {
        return Map.of(
                "type", "ARRAY",
                "items", itemSchema
        );
    }

    private GeminiConfig getGeminiConfig() {
        String apiKey = getProperty(API_KEY_PROPERTY, getProperty(API_KEY_ENV_PROPERTY, ""));
        String apiBaseUrl = getProperty(API_BASE_URL_PROPERTY, DEFAULT_API_BASE_URL);
        String model = getProperty(MODEL_PROPERTY, DEFAULT_MODEL);
        return new GeminiConfig(apiKey, apiBaseUrl, model);
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

    private static boolean isConfigured(String value) {
        return StringUtils.hasText(value) && !isUnresolvedPlaceholder(value);
    }

    private static boolean isUnresolvedPlaceholder(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.startsWith("${") && trimmedValue.endsWith("}");
    }

    private static RestClient createTimeoutRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MILLIS));
        requestFactory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MILLIS));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private record GeminiConfig(String apiKey, String apiBaseUrl, String model) {

        private boolean apiKeyPresent() {
            return isConfigured(apiKey);
        }

        private boolean baseUrlPresent() {
            return isConfigured(apiBaseUrl);
        }

        private boolean modelPresent() {
            return isConfigured(model);
        }

        private String generateContentUrl() {
            String normalizedBaseUrl = apiBaseUrl.endsWith("/")
                    ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1)
                    : apiBaseUrl;
            String normalizedModel = model.startsWith("models/") ? model.substring("models/".length()) : model;
            return normalizedBaseUrl + "/" + normalizedModel + ":generateContent";
        }

        private void validate() {
            if (!apiKeyPresent()) {
                throw new IllegalStateException("Gemini API key is not configured");
            }
            if (!baseUrlPresent()) {
                throw new IllegalStateException("Gemini API base URL is not configured");
            }
            if (!modelPresent()) {
                throw new IllegalStateException("Gemini model is not configured");
            }
        }
    }

    static class AiResponseValidationException extends Exception {

        private AiResponseValidationException(String message) {
            super(message);
        }

        private AiResponseValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
