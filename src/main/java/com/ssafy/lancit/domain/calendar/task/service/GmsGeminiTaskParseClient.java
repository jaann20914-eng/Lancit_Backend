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

@Component
public class GmsGeminiTaskParseClient implements AiTaskParseClient {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;
    private static final String API_KEY_PROPERTY = "gms.api.key";
    private static final String GEMINI_URL_PROPERTY = "gms.gemini.url";
    private static final String PROVIDER_PROPERTY = "gms.provider";
    private static final List<String> REQUIRED_RESPONSE_FIELDS = List.of(
            "sourceText",
            "categoryId",
            "title",
            "content",
            "memo",
            "startAt",
            "endAt",
            "status",
            "clientCompany",
            "budget",
            "paidAt",
            "confidence",
            "warnings"
    );

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
            return requestAndParse(config, createPrompt(sourceText));
        } catch (GeminiResponseValidationException firstFailure) {
            try {
                return requestAndParse(config, createRetryPrompt(sourceText, firstFailure.getMessage()));
            } catch (GeminiResponseValidationException retryFailure) {
                throw new IllegalStateException("Failed to parse GMS Gemini response after retry", retryFailure);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse GMS Gemini retry response", e);
            }
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

    private TaskParseResponseDTO requestAndParse(GeminiConfig config, String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(createRequestBody(prompt));
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
        return parseTaskResponse(aiText);
    }

    private Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
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
                사용자의 자연어 일정 문장을 분석해서 아래 TaskParseResponseDTO JSON 스키마와 동일한 필드명만 사용해 응답하세요.
                모든 응답은 JSON 객체만 반환하세요. 설명 문장, 마크다운, 코드블록, 접두사, 접미사를 절대 출력하지 마세요.

                기준 날짜: %s
                기준 시간대: Asia/Seoul

                반환 형식:
                - 모든 DTO 필드를 반드시 포함하세요.
                - 알 수 없는 값은 null로 반환하세요.
                - 배열 필드가 있다면 값이 없을 때 빈 배열로 반환하세요.
                - sourceText는 사용자의 원문을 그대로 반환하세요.
                - startAt, endAt, paidAt은 DTO 변환을 위해 ISO-8601 LocalDateTime 문자열(예: 2026-07-01T14:00:00) 또는 null로 반환하세요.
                - categoryId는 항상 null입니다. 사용자가 카테고리를 말해도 추정하지 마세요.
                - status는 기본값으로 "IN_PROGRESS"를 사용하세요.
                - confidence는 0.0 이상 1.0 이하 숫자로 반환하세요.
                - warnings는 확인이 필요한 내용이 있으면 한국어 문자열 배열로, 없으면 빈 배열로 반환하세요.
                - 저장, 등록, 수정 같은 동작은 하지 말고 파싱 결과 DTO만 반환하세요.

                필드별 판단 기준:
                - title: 일정 제목입니다. 날짜, 시간, 장소, 금액, 지급일 표현은 제거하고 핵심 명사구로 만드세요.
                - content: 일정의 핵심 내용입니다. 원문 전체를 단순 복사하지 말고, 일정 자체를 설명하는 내용만 넣으세요.
                - memo: 장소, 링크, 준비물, 회의실, 회의 방식, 부가 설명 등 기존 주요 필드에 들어가지 않는 정보를 넣으세요.
                - startAt: 실제 회의/작업/일정의 시작 일시입니다. 지급일이나 입금일을 startAt으로 넣지 마세요.
                - endAt: 명시된 종료 시간 또는 기간이 있을 때만 계산하세요. 시작 시간과 기간이 함께 있으면 endAt을 계산하세요.
                - paidAt: 입금, 정산, 지급 예정일 또는 지급 기한입니다.
                - budget: 금액, 전체 예산, 비용입니다.
                - clientCompany: 사람 이름이나 장소가 아니라 회사명/거래처명만 넣으세요.
                - categoryId: 항상 null입니다.

                중요 규칙:
                - 상대 날짜 표현(오늘, 내일, 다음 주 등)은 기준 날짜와 Asia/Seoul 기준으로 해석하세요.
                - 날짜/시간은 사용자가 말한 정보만 기준으로 파싱하세요.
                - 사용자가 말하지 않은 날짜, 시간, 회사명, 금액, 카테고리는 추정하지 마세요.
                - 날짜/시간이 모호하면 임의 추정하지 말고 null로 반환하세요.
                - "SSAFY 1층 회의실", "카페", "회의실", "온라인", "줌 링크", "Zoom", "주소", "강남역 카페" 등은 clientCompany가 아니라 memo입니다.
                - "삼성전자", "네이버", "카카오", "A사", "의뢰사"처럼 거래처나 회사로 판단되는 경우만 clientCompany에 넣으세요.
                - 장소 필드를 따로 만들지 않습니다. 장소성 정보는 memo에 넣으세요.
                - clientCompany에 장소, 회의실, 학교, 층수, 카페, 온라인 회의 링크를 넣지 마세요.
                - memo에는 장소, 링크, 준비물, 회의 방식, 기타 부가 정보를 넣으세요.
                - 일정일과 지급일이 함께 있으면 일정일은 startAt, 지급일은 paidAt으로 분리하세요.
                - 입금/정산/지급 중심 문장은 회의나 작업 일정이 아니므로 startAt/endAt을 억지로 채우지 마세요.
                - 날짜 없이 시간만 있으면 startAt은 null로 두고 시간 표현은 memo에 남기세요.
                - 사용자가 말하지 않은 값은 추정하지 마세요.

                Few-shot 예시:
                아래 예시는 필드 배치 판단 기준입니다. 실제 응답에서는 위 반환 형식에 따라 startAt/endAt/paidAt을 ISO-8601 LocalDateTime 문자열 또는 null로 반환하세요.

                1. 일정일과 지급일이 같이 있는 문장
                입력: "7월 1일 오후 2시에 삼성전자와 계약 미팅하고, 7월 5일에 300만원 입금 예정"
                기대 결과:
                {
                  "sourceText": "7월 1일 오후 2시에 삼성전자와 계약 미팅하고, 7월 5일에 300만원 입금 예정",
                  "categoryId": null,
                  "title": "계약 미팅",
                  "content": "삼성전자와 계약 미팅",
                  "memo": null,
                  "startAt": "7월 1일 오후 2시",
                  "endAt": null,
                  "status": "IN_PROGRESS",
                  "clientCompany": "삼성전자",
                  "budget": 3000000,
                  "paidAt": "7월 5일",
                  "confidence": 0.95,
                  "warnings": []
                }
                판단 기준: 실제 일정은 "7월 1일 오후 2시 계약 미팅"이므로 startAt에 들어갑니다. 입금 예정일은 일정 시작일이 아니라 지급일이므로 paidAt에 들어갑니다. 300만원은 금액 정보이므로 budget에 들어갑니다. 삼성전자는 회사/거래처명이므로 clientCompany에 들어갑니다.

                2. 날짜만 있고 시간이 없는 문장
                입력: "7월 3일 네이버 프로젝트 킥오프 미팅"
                기대 결과:
                {
                  "sourceText": "7월 3일 네이버 프로젝트 킥오프 미팅",
                  "categoryId": null,
                  "title": "프로젝트 킥오프 미팅",
                  "content": "네이버 프로젝트 킥오프 미팅",
                  "memo": null,
                  "startAt": "7월 3일",
                  "endAt": null,
                  "status": "IN_PROGRESS",
                  "clientCompany": "네이버",
                  "budget": null,
                  "paidAt": null,
                  "confidence": 0.86,
                  "warnings": ["시간이 명시되지 않았습니다."]
                }
                판단 기준: 날짜는 명시되어 있으므로 startAt에 넣습니다. 시간이 없으므로 임의로 오전 9시, 자정 등으로 추정하지 않습니다. 종료 시간이 없으므로 endAt은 null입니다.

                3. 시간만 있고 날짜가 없는 문장
                입력: "오후 3시에 포트폴리오 수정하기"
                기대 결과:
                {
                  "sourceText": "오후 3시에 포트폴리오 수정하기",
                  "categoryId": null,
                  "title": "포트폴리오 수정",
                  "content": "포트폴리오 수정하기",
                  "memo": "시간만 명시됨: 오후 3시",
                  "startAt": null,
                  "endAt": null,
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": null,
                  "paidAt": null,
                  "confidence": 0.62,
                  "warnings": ["날짜가 명시되지 않았습니다."]
                }
                판단 기준: 날짜 없이 시간만 있는 경우 정확한 일정 일시를 만들 수 없으므로 startAt은 null입니다. 시간 정보는 버리지 말고 memo에 남깁니다. 사용자가 말하지 않은 날짜를 오늘/내일로 추정하지 않습니다.

                4. "다음 주 금요일까지 입금" 문장
                입력: "다음 주 금요일까지 300만원 입금 예정"
                기대 결과:
                {
                  "sourceText": "다음 주 금요일까지 300만원 입금 예정",
                  "categoryId": null,
                  "title": "입금 예정",
                  "content": "300만원 입금 예정",
                  "memo": null,
                  "startAt": null,
                  "endAt": null,
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": 3000000,
                  "paidAt": "다음 주 금요일",
                  "confidence": 0.9,
                  "warnings": []
                }
                판단 기준: 이 문장은 회의나 작업 일정이 아니라 지급/입금 관련 문장입니다. 따라서 startAt, endAt을 무리하게 채우지 않습니다. "다음 주 금요일까지"는 지급 기한이므로 paidAt에 넣습니다. 300만원은 금액이므로 budget에 넣습니다.

                5. "내일 오후 3시부터 2시간" 문장
                입력: "내일 오후 3시부터 2시간 동안 포트폴리오 수정"
                기대 결과:
                {
                  "sourceText": "내일 오후 3시부터 2시간 동안 포트폴리오 수정",
                  "categoryId": null,
                  "title": "포트폴리오 수정",
                  "content": "포트폴리오 수정",
                  "memo": null,
                  "startAt": "내일 오후 3시",
                  "endAt": "내일 오후 5시",
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": null,
                  "paidAt": null,
                  "confidence": 0.95,
                  "warnings": []
                }
                판단 기준: 시작 시간이 명시되어 있으므로 startAt에 넣습니다. "2시간 동안"이라는 기간이 있으므로 시작 시간 기준으로 endAt을 계산합니다. 지급/입금 관련 표현이 없으므로 paidAt은 null입니다.

                6. 회사명이 접미사 없이 나오는 문장
                입력: "내일 오전 10시에 카카오 미팅"
                기대 결과:
                {
                  "sourceText": "내일 오전 10시에 카카오 미팅",
                  "categoryId": null,
                  "title": "미팅",
                  "content": "카카오 미팅",
                  "memo": null,
                  "startAt": "내일 오전 10시",
                  "endAt": null,
                  "status": "IN_PROGRESS",
                  "clientCompany": "카카오",
                  "budget": null,
                  "paidAt": null,
                  "confidence": 0.92,
                  "warnings": []
                }
                판단 기준: "카카오"처럼 주식회사, 회사, 기업, 거래처 등의 접미사가 없어도 회사명으로 판단 가능한 경우 clientCompany에 넣습니다. 단, "SSAFY 1층 회의실", "강남역 카페", "온라인 줌"처럼 장소성 표현은 회사명이 아니므로 clientCompany에 넣지 않습니다.

                7. 예산과 지급 금액이 헷갈리는 문장
                입력: "7월 10일 롯데와 광고 영상 회의, 전체 예산은 500만원이고 계약금 200만원은 7월 15일 지급 예정"
                기대 결과:
                {
                  "sourceText": "7월 10일 롯데와 광고 영상 회의, 전체 예산은 500만원이고 계약금 200만원은 7월 15일 지급 예정",
                  "categoryId": null,
                  "title": "광고 영상 회의",
                  "content": "롯데와 광고 영상 회의",
                  "memo": "계약금 200만원 지급 예정",
                  "startAt": "7월 10일",
                  "endAt": null,
                  "status": "IN_PROGRESS",
                  "clientCompany": "롯데",
                  "budget": 5000000,
                  "paidAt": "7월 15일",
                  "confidence": 0.94,
                  "warnings": []
                }
                판단 기준: 전체 예산은 budget에 넣습니다. 지급 예정일은 paidAt에 넣습니다. 계약금처럼 예산과 별도의 지급 세부 금액은 DTO에 별도 필드가 없다면 memo에 남깁니다. 회의 날짜와 지급 날짜를 혼동하지 않습니다.

                8. 장소가 회사명처럼 보일 수 있는 문장
                입력: "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의"
                기대 결과:
                {
                  "sourceText": "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의",
                  "categoryId": null,
                  "title": "팀 회의",
                  "content": "팀 회의",
                  "memo": "SSAFY 1층 회의실",
                  "startAt": "내일 오후 3시",
                  "endAt": null,
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": null,
                  "paidAt": null,
                  "confidence": 0.93,
                  "warnings": []
                }
                판단 기준: "SSAFY 1층 회의실"은 거래처명이 아니라 장소 정보입니다. 장소 정보는 clientCompany가 아니라 memo에 넣습니다. 장소 필드를 새로 만들지 않습니다.

                9. 링크와 준비물이 포함된 문장
                입력: "6월 20일 오후 4시 온라인 회의, 줌 링크는 회의 전에 공유, 포트폴리오 PDF 준비"
                기대 결과:
                {
                  "sourceText": "6월 20일 오후 4시 온라인 회의, 줌 링크는 회의 전에 공유, 포트폴리오 PDF 준비",
                  "categoryId": null,
                  "title": "온라인 회의",
                  "content": "온라인 회의",
                  "memo": "줌 링크는 회의 전에 공유, 포트폴리오 PDF 준비",
                  "startAt": "6월 20일 오후 4시",
                  "endAt": null,
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": null,
                  "paidAt": null,
                  "confidence": 0.93,
                  "warnings": []
                }
                판단 기준: 링크, 준비물, 부가 설명은 memo에 넣습니다. 회사/거래처가 명시되지 않았으므로 clientCompany는 null입니다.

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

    private String createRetryPrompt(String sourceText, String failureReason) {
        return """
                이전 응답은 DTO 스키마에 맞지 않습니다.
                아래 스키마의 모든 필드를 포함한 JSON만 다시 반환하세요.
                설명 문장은 출력하지 마세요.
                이전 응답 검증 실패 사유: %s

                %s
                """.formatted(failureReason, createPrompt(sourceText));
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
            throw new GeminiResponseValidationException("GMS Gemini response text is empty");
        }
        return textNode.asString();
    }

    private TaskParseResponseDTO parseTaskResponse(String aiText) throws GeminiResponseValidationException {
        String json = extractJson(aiText);
        validateJsonSchema(json);
        try {
            TaskParseResponseDTO responseDTO = objectMapper.readValue(json, TaskParseResponseDTO.class);
            validateParsedDto(responseDTO);
            return responseDTO;
        } catch (GeminiResponseValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiResponseValidationException("DTO conversion failed", e);
        }
    }

    private String extractJson(String aiText) throws GeminiResponseValidationException {
        String trimmedText = aiText.trim();

        if (!trimmedText.startsWith("{") || !trimmedText.endsWith("}")) {
            throw new GeminiResponseValidationException("AI response is not a JSON object");
        }
        return trimmedText;
    }

    @SuppressWarnings("unchecked")
    private void validateJsonSchema(String json) throws GeminiResponseValidationException {
        Map<String, Object> responseMap;
        try {
            Object parsedJson = objectMapper.readValue(json, Map.class);
            if (!(parsedJson instanceof Map<?, ?> parsedMap)) {
                throw new GeminiResponseValidationException("AI response JSON is not an object");
            }
            responseMap = (Map<String, Object>) parsedMap;
        } catch (GeminiResponseValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiResponseValidationException("AI response is not valid JSON", e);
        }

        List<String> missingFields = REQUIRED_RESPONSE_FIELDS.stream()
                .filter(field -> !responseMap.containsKey(field))
                .toList();
        if (!missingFields.isEmpty()) {
            throw new GeminiResponseValidationException("AI response is missing required fields: " + missingFields);
        }

        List<String> unknownFields = responseMap.keySet().stream()
                .map(String::valueOf)
                .filter(field -> !REQUIRED_RESPONSE_FIELDS.contains(field))
                .toList();
        if (!unknownFields.isEmpty()) {
            throw new GeminiResponseValidationException("AI response contains unknown fields: " + unknownFields);
        }

        if (responseMap.get("categoryId") != null) {
            throw new GeminiResponseValidationException("categoryId must be null");
        }
        if (!(responseMap.get("warnings") instanceof List<?>)) {
            throw new GeminiResponseValidationException("warnings must be an array");
        }
    }

    private void validateParsedDto(TaskParseResponseDTO responseDTO) throws GeminiResponseValidationException {
        if (responseDTO == null) {
            throw new GeminiResponseValidationException("AI task parse result is empty");
        }
        if (!StringUtils.hasText(responseDTO.getTitle())) {
            throw new GeminiResponseValidationException("AI task parse result has no title");
        }
        if (responseDTO.getStatus() == null) {
            throw new GeminiResponseValidationException("AI task parse result has no status");
        }
        if (responseDTO.getCategoryId() != null) {
            throw new GeminiResponseValidationException("categoryId must be null");
        }
        if (responseDTO.getWarnings() == null) {
            throw new GeminiResponseValidationException("warnings must be an array");
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

    private record GeminiConfig(String apiKey, String geminiUrl) {
    }

    private static class GeminiResponseValidationException extends Exception {

        private GeminiResponseValidationException(String message) {
            super(message);
        }

        private GeminiResponseValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
