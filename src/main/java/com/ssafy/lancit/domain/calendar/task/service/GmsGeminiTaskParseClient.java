package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GmsGeminiTaskParseClient implements AiTaskParseClient {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;
    private static final String API_KEY_PROPERTY = "gms.api.key";
    private static final String GEMINI_URL_PROPERTY = "gms.gemini.url";
    private static final String PROVIDER_PROPERTY = "gms.provider";
    private static final Pattern JSON_CODE_BLOCK_PATTERN =
            Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Autowired
    public GmsGeminiTaskParseClient(ObjectMapper objectMapper, Environment environment) {
        this(createTimeoutRestClient(), objectMapper, environment);
    }

    GmsGeminiTaskParseClient(ObjectMapper objectMapper) {
        this(createTimeoutRestClient(), objectMapper, null);
    }

    GmsGeminiTaskParseClient(RestClient restClient, ObjectMapper objectMapper, Environment environment) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    public TaskParseResponseDTO parse(String sourceText) {
        GeminiConfig config = getGeminiConfig();

        try {
            String requestBody = objectMapper.writeValueAsString(createRequestBody(sourceText));
            ResponseEntity<String> response = restClient.post()
                    .uri(config.geminiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", config.apiKey())
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                throw new IllegalStateException("GMS Gemini returned an empty or non-success response");
            }

            String aiText = extractGeminiText(response.getBody());
            String json = extractJson(aiText);
            return objectMapper.readValue(json, TaskParseResponseDTO.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse GMS Gemini response", e);
        }
    }

    private GeminiConfig getGeminiConfig() {
        String provider = getProperty(PROVIDER_PROPERTY, "gemini");
        String apiKey = getProperty(API_KEY_PROPERTY, "");
        String geminiUrl = getProperty(GEMINI_URL_PROPERTY, "");

        if (!"gemini".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("AI task parse provider is not gemini");
        }
        if (!StringUtils.hasText(apiKey) || isUnresolvedPlaceholder(apiKey)) {
            throw new IllegalStateException("GMS API key is not configured");
        }
        if (!StringUtils.hasText(geminiUrl) || isUnresolvedPlaceholder(geminiUrl)) {
            throw new IllegalStateException("GMS Gemini URL is not configured");
        }
        return new GeminiConfig(apiKey, geminiUrl);
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

    private boolean isUnresolvedPlaceholder(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.startsWith("${") && trimmedValue.endsWith("}");
    }

    private Map<String, Object> createRequestBody(String sourceText) {
        return Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", createPrompt(sourceText)))
                )),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json"
                )
        );
    }

    private String createPrompt(String sourceText) {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        return """
                사용자의 자연어 일정 문장을 분석해서 아래 JSON 스키마와 동일한 필드명만 사용해 응답하세요.
                설명 문장, 마크다운, 코드블록 없이 JSON 객체만 반환하세요.

                기준 날짜: %s
                기준 시간대: Asia/Seoul

                규칙:
                - 상대 날짜 표현(오늘, 내일, 다음 주 등)은 기준 날짜와 Asia/Seoul 기준으로 해석하세요.
                - 날짜/시간 값은 ISO-8601 LocalDateTime 문자열(예: 2026-06-09T15:00:00)로 반환하세요.
                - 알 수 없는 값은 추측하지 말고 null로 반환하세요.
                - sourceText는 사용자의 원문을 그대로 반환하세요.
                - content는 일정의 주요 내용/설명입니다. 원문 전체를 단순 복사하지 마세요.
                - memo는 정형 필드에 들어가지 않는 부가 정보입니다. 장소, 온라인 링크, 준비물, 참고사항 등을 넣으세요.
                - clientCompany는 의뢰 회사, 고객사, 발주처가 명확히 언급된 경우에만 채우세요.
                - 장소 정보는 clientCompany가 아니라 memo에 포함하세요.
                - "SSAFY 1층 회의실", "1층", "회의실", "강남역 카페", "온라인", "Zoom", "주소"는 clientCompany로 반환하지 말고 memo로 반환하세요.
                - categoryId는 사용자별 카테고리 조회가 필요하므로 null로 반환하세요.
                - status는 기본값으로 "IN_PROGRESS"를 사용하세요.
                - confidence는 0.0 이상 1.0 이하 숫자로 반환하세요.
                - warnings는 확인이 필요한 내용이 있으면 한국어 문자열 배열로, 없으면 빈 배열로 반환하세요.
                - 저장, 등록, 수정 같은 동작은 하지 말고 파싱 결과만 반환하세요.

                JSON 필드:
                {
                  "sourceText": string,
                  "categoryId": number|null,
                  "title": string|null,
                  "content": string|null,
                  "memo": string|null,
                  "startAt": string|null,
                  "endAt": string|null,
                  "status": "IN_PROGRESS"|"COMPLETED"|"CANCELLED"|null,
                  "clientCompany": string|null,
                  "budget": number|null,
                  "paidAt": string|null,
                  "confidence": number|null,
                  "warnings": string[]
                }

                사용자 원문:
                %s
                """.formatted(today, sourceText);
    }

    private String extractGeminiText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (!textNode.isTextual() || !StringUtils.hasText(textNode.asString())) {
            throw new IllegalStateException("GMS Gemini response text is empty");
        }
        return textNode.asString();
    }

    private String extractJson(String aiText) {
        String trimmedText = aiText.trim();
        Matcher codeBlockMatcher = JSON_CODE_BLOCK_PATTERN.matcher(trimmedText);
        if (codeBlockMatcher.find()) {
            trimmedText = codeBlockMatcher.group(1).trim();
        }

        int jsonStart = trimmedText.indexOf('{');
        if (jsonStart < 0) {
            throw new IllegalStateException("AI response does not contain a JSON object");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int i = jsonStart; i < trimmedText.length(); i++) {
            char current = trimmedText.charAt(i);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return trimmedText.substring(jsonStart, i + 1);
                }
            }
        }

        throw new IllegalStateException("AI response JSON object is incomplete");
    }

    private static RestClient createTimeoutRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MILLIS));
        requestFactory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MILLIS));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private record GeminiConfig(String apiKey, String geminiUrl) {
    }
}
