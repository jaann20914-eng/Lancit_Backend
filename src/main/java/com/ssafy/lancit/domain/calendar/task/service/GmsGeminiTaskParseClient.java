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

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GmsGeminiTaskParseClient implements AiTaskParseClient {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final String API_KEY_PROPERTY = "gms.api.key";
    private static final String AI_URL_PROPERTY = "gms.ai.url";
    private static final String AI_MODEL_PROPERTY = "gms.ai.model";
    private static final String LEGACY_GEMINI_URL_PROPERTY = "gms.gemini.url";
    private static final String LEGACY_GEMINI_MODEL_PROPERTY = "gms.gemini.model";
    private static final String DEFAULT_AI_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/responses";
    private static final String DEFAULT_AI_MODEL = "gpt-5.5-pro";
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
        GmsAiConfig config = getGmsAiConfig();

        try {
            config.validate();
            TaskParseResponseDTO responseDTO = requestAndParseWithRetry(config, sourceText);
            log.info("GMS AI task parsing succeeded. model={}, urlPresent={}, apiKeyPresent={}",
                    config.model(), config.urlPresent(), config.apiKeyPresent());
            return responseDTO;
        } catch (RuntimeException e) {
            log.warn("GMS AI task parsing failed. model={}, urlPresent={}, apiKeyPresent={}",
                    config.model(), config.urlPresent(), config.apiKeyPresent());
            throw e;
        } catch (Exception e) {
            log.warn("GMS AI task parsing failed. model={}, urlPresent={}, apiKeyPresent={}",
                    config.model(), config.urlPresent(), config.apiKeyPresent());
            throw new IllegalStateException("Failed to parse GMS AI response", e);
        }
    }

    private TaskParseResponseDTO requestAndParseWithRetry(GmsAiConfig config, String sourceText) throws Exception {
        try {
            return requestAndParse(config, createPrompt(sourceText));
        } catch (AiResponseValidationException firstFailure) {
            try {
                return requestAndParse(config, createRetryPrompt(sourceText, firstFailure.getMessage()));
            } catch (AiResponseValidationException retryFailure) {
                throw new IllegalStateException("Failed to parse GMS AI response after retry", retryFailure);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse GMS AI retry response", e);
            }
        }
    }

    private GmsAiConfig getGmsAiConfig() {
        String apiKey = getProperty(API_KEY_PROPERTY, "");
        String url = getProperty(AI_URL_PROPERTY, getProperty(LEGACY_GEMINI_URL_PROPERTY, DEFAULT_AI_URL));
        String model = getProperty(AI_MODEL_PROPERTY, getProperty(LEGACY_GEMINI_MODEL_PROPERTY, DEFAULT_AI_MODEL));

        return new GmsAiConfig(apiKey, url, model);
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

    private TaskParseResponseDTO requestAndParse(GmsAiConfig config, String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(createRequestBody(config, prompt));
        ResponseEntity<String> response = restClient.post()
                .uri(config.url())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + config.apiKey())
                .body(requestBody)
                .retrieve()
                .toEntity(String.class);

        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            throw new IllegalStateException("GMS AI returned an empty or non-success response");
        }

        String aiText = extractResponsesText(response.getBody());
        return parseTaskResponse(aiText);
    }

    private Map<String, Object> createRequestBody(GmsAiConfig config, String prompt) {
        return Map.of(
                "model", config.model(),
                "input", prompt
        );
    }

    private String createPrompt(String sourceText) {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        return """
                사용자의 자연어 일정 문장을 분석해서 아래 TaskParseResponseDTO JSON 스키마와 동일한 필드명만 사용해 응답하세요.
                최종 응답은 JSON 객체 하나만 반환하세요. 설명 문장, 마크다운, 코드블록, 접두사, 접미사를 절대 출력하지 마세요.
                JSON 객체의 첫 글자는 { 이고 마지막 글자는 } 이어야 합니다.
                DTO에 없는 필드는 절대 추가하지 말고, DTO의 모든 필드는 반드시 포함하세요.
                content는 null로 두지 말고 사용자의 원문에서 실제 일정/작업/지급 내용을 한 문장으로 요약하세요.

                기준 날짜: %s
                기준 시간대: Asia/Seoul

                반환 형식:
                - 모든 DTO 필드를 반드시 포함하세요.
                - 알 수 없는 값은 null로 반환하세요.
                - 배열 필드가 있다면 값이 없을 때 빈 배열로 반환하세요.
                - sourceText는 사용자의 원문을 그대로 반환하세요.
                - startAt, endAt, paidAt은 날짜와 시간이 모두 확정된 경우에만 ISO-8601 LocalDateTime 문자열(예: 2026-07-01T14:00:00)로 반환하세요.
                - 날짜만 확정된 경우 startAt/endAt/paidAt은 null로 두고 startDate/endDate/paidDate에 ISO-8601 LocalDate 문자열(예: 2026-07-01)을 넣으세요.
                - 시간만 확정된 경우 startAt/endAt/paidAt은 null로 두고 startTime/endTime/paidTime에 ISO-8601 LocalTime 문자열(예: 15:00:00)을 넣으세요.
                - startText, endText, paidText에는 사용자가 말한 날짜/시간 표현을 원문 그대로 보존하세요.
                - startPrecision, endPrecision, paidPrecision은 "NONE", "DATE_ONLY", "TIME_ONLY", "DATE_TIME" 중 하나로 반환하세요.
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
                - budget: 하위 호환용 대표 금액입니다. 전체 예산이 있으면 budgetAmount와 같은 값을 넣고, 없으면 paidAmount, depositAmount, balanceAmount, contractAmount 중 가장 핵심 금액을 넣으세요.
                - budgetAmount: 전체 예산, 총예산, 견적 금액입니다.
                - depositAmount: 계약금, 선금입니다.
                - paidAmount: 실제 입금액, 지급액, 비용 지급 금액입니다.
                - balanceAmount: 잔금입니다. 잔금이 특정 지급일과 함께 입금/지급될 때는 paidAmount에도 같은 금액을 넣을 수 있습니다.
                - contractAmount: 계약 총액, 계약 총금액입니다.
                - budgetText: "100~150만원", "선금 30%%"처럼 단일 숫자로 확정하기 어려운 금액 범위/비율 원문입니다.
                - clientCompany: 사람 이름이나 장소가 아니라 회사명/거래처명만 넣으세요.
                - categoryId: 항상 null입니다.

                날짜/시간 정밀도 정책:
                - 날짜와 시간이 모두 명시되거나 상대 날짜 기준으로 모두 확정되면 *At, *Date, *Time, *Text, *Precision=DATE_TIME을 모두 채우세요.
                - 날짜만 있으면 *At과 *Time은 null, *Date는 채우고, *Text는 원문 날짜 표현, *Precision=DATE_ONLY로 반환하세요.
                - 시간만 있으면 *At과 *Date는 null, *Time은 채우고, *Text는 원문 시간 표현, *Precision=TIME_ONLY로 반환하세요.
                - 날짜/시간 표현이 없으면 *At, *Date, *Time, *Text는 null, *Precision=NONE으로 반환하세요.
                - 날짜만 있는 일정에 00:00, 09:00 같은 시간을 임의로 붙이지 마세요.
                - 시간만 있는 일정에 오늘, 내일 같은 날짜를 임의로 붙이지 마세요.
                - 입금/정산/지급 중심 문장의 날짜는 paidDate 또는 paidAt에만 넣고 startDate/startAt에는 넣지 마세요.
                - startAt이 null이어도 startDate/startTime/startText/startPrecision으로 사용자가 말한 정보를 잃지 마세요.

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
                - 전체 예산과 계약금/잔금/입금액이 같이 있으면 서로 다른 금액 필드로 분리하세요.
                - DTO에 들어가지 않는 금액 세부 설명(예: 계약금 지급 예정, 부가세 포함, 선금 30%%/잔금 70%%)은 memo 또는 budgetText에 보존하세요.
                - 사용자가 말하지 않은 값은 추정하지 마세요.

                Few-shot 예시:
                아래 예시는 필드 배치 판단 기준입니다. 실제 응답에서는 위 반환 형식에 따라 *At은 ISO-8601 LocalDateTime, *Date는 ISO-8601 LocalDate, *Time은 ISO-8601 LocalTime 문자열 또는 null로 반환하세요.
                예시에 일부 신규 금액 필드가 생략되어 있어도 실제 응답에서는 모든 DTO 필드를 null 또는 적절한 값으로 반드시 포함하세요.

                1. 일정일과 지급일이 같이 있는 문장
                입력: "7월 1일 오후 2시에 삼성전자와 계약 미팅하고, 7월 5일에 300만원 입금 예정"
                기대 결과:
                {
                  "sourceText": "7월 1일 오후 2시에 삼성전자와 계약 미팅하고, 7월 5일에 300만원 입금 예정",
                  "categoryId": null,
                  "title": "계약 미팅",
                  "content": "삼성전자와 계약 미팅",
                  "memo": null,
                  "startAt": "2026-07-01T14:00:00",
                  "startDate": "2026-07-01",
                  "startTime": "14:00:00",
                  "startText": "7월 1일 오후 2시",
                  "startPrecision": "DATE_TIME",
                  "endAt": null,
                  "endDate": null,
                  "endTime": null,
                  "endText": null,
                  "endPrecision": "NONE",
                  "status": "IN_PROGRESS",
                  "clientCompany": "삼성전자",
                  "budget": 3000000,
                  "paidAt": null,
                  "paidDate": "2026-07-05",
                  "paidTime": null,
                  "paidText": "7월 5일",
                  "paidPrecision": "DATE_ONLY",
                  "confidence": 0.95,
                  "warnings": []
                }
                판단 기준: 실제 일정은 "7월 1일 오후 2시 계약 미팅"이므로 startAt에 들어갑니다. 입금 예정일은 일정 시작일이 아니라 지급일이므로 paidDate에 들어갑니다. 300만원은 금액 정보이므로 budget에 들어갑니다. 삼성전자는 회사/거래처명이므로 clientCompany에 들어갑니다.

                2. 날짜만 있고 시간이 없는 문장
                입력: "7월 3일 네이버 프로젝트 킥오프 미팅"
                기대 결과:
                {
                  "sourceText": "7월 3일 네이버 프로젝트 킥오프 미팅",
                  "categoryId": null,
                  "title": "프로젝트 킥오프 미팅",
                  "content": "네이버 프로젝트 킥오프 미팅",
                  "memo": null,
                  "startAt": null,
                  "startDate": "2026-07-03",
                  "startTime": null,
                  "startText": "7월 3일",
                  "startPrecision": "DATE_ONLY",
                  "endAt": null,
                  "endDate": null,
                  "endTime": null,
                  "endText": null,
                  "endPrecision": "NONE",
                  "status": "IN_PROGRESS",
                  "clientCompany": "네이버",
                  "budget": null,
                  "paidAt": null,
                  "paidDate": null,
                  "paidTime": null,
                  "paidText": null,
                  "paidPrecision": "NONE",
                  "confidence": 0.86,
                  "warnings": ["시간이 명시되지 않았습니다."]
                }
                판단 기준: 날짜는 명시되어 있으므로 startDate에 넣습니다. 시간이 없으므로 startAt에 임의로 오전 9시, 자정 등을 넣지 않습니다. 종료 시간이 없으므로 endAt은 null입니다.

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
                  "startDate": null,
                  "startTime": "15:00:00",
                  "startText": "오후 3시",
                  "startPrecision": "TIME_ONLY",
                  "endAt": null,
                  "endDate": null,
                  "endTime": null,
                  "endText": null,
                  "endPrecision": "NONE",
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": null,
                  "paidAt": null,
                  "paidDate": null,
                  "paidTime": null,
                  "paidText": null,
                  "paidPrecision": "NONE",
                  "confidence": 0.62,
                  "warnings": ["날짜가 명시되지 않았습니다."]
                }
                판단 기준: 날짜 없이 시간만 있는 경우 정확한 일정 일시를 만들 수 없으므로 startAt은 null입니다. 시간 정보는 startTime/startText와 memo에 남깁니다. 사용자가 말하지 않은 날짜를 오늘/내일로 추정하지 않습니다.

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
                  "startDate": null,
                  "startTime": null,
                  "startText": null,
                  "startPrecision": "NONE",
                  "endAt": null,
                  "endDate": null,
                  "endTime": null,
                  "endText": null,
                  "endPrecision": "NONE",
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": 3000000,
                  "paidAt": null,
                  "paidDate": "2026-06-19",
                  "paidTime": null,
                  "paidText": "다음 주 금요일",
                  "paidPrecision": "DATE_ONLY",
                  "confidence": 0.9,
                  "warnings": []
                }
                판단 기준: 이 문장은 회의나 작업 일정이 아니라 지급/입금 관련 문장입니다. 따라서 startAt, endAt을 무리하게 채우지 않습니다. "다음 주 금요일까지"는 지급 기한이므로 paidDate에 넣습니다. 300만원은 금액이므로 budget에 넣습니다.

                5. "내일 오후 3시부터 2시간" 문장
                입력: "내일 오후 3시부터 2시간 동안 포트폴리오 수정"
                기대 결과:
                {
                  "sourceText": "내일 오후 3시부터 2시간 동안 포트폴리오 수정",
                  "categoryId": null,
                  "title": "포트폴리오 수정",
                  "content": "포트폴리오 수정",
                  "memo": null,
                  "startAt": "2026-06-11T15:00:00",
                  "startDate": "2026-06-11",
                  "startTime": "15:00:00",
                  "startText": "내일 오후 3시",
                  "startPrecision": "DATE_TIME",
                  "endAt": "2026-06-11T17:00:00",
                  "endDate": "2026-06-11",
                  "endTime": "17:00:00",
                  "endText": "2시간 동안",
                  "endPrecision": "DATE_TIME",
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": null,
                  "paidAt": null,
                  "paidDate": null,
                  "paidTime": null,
                  "paidText": null,
                  "paidPrecision": "NONE",
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
                  "startAt": "2026-06-11T10:00:00",
                  "startDate": "2026-06-11",
                  "startTime": "10:00:00",
                  "startText": "내일 오전 10시",
                  "startPrecision": "DATE_TIME",
                  "endAt": null,
                  "endDate": null,
                  "endTime": null,
                  "endText": null,
                  "endPrecision": "NONE",
                  "status": "IN_PROGRESS",
                  "clientCompany": "카카오",
                  "budget": null,
                  "paidAt": null,
                  "paidDate": null,
                  "paidTime": null,
                  "paidText": null,
                  "paidPrecision": "NONE",
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
                  "startAt": null,
                  "startDate": "2026-07-10",
                  "startTime": null,
                  "startText": "7월 10일",
                  "startPrecision": "DATE_ONLY",
                  "endAt": null,
                  "endDate": null,
                  "endTime": null,
                  "endText": null,
                  "endPrecision": "NONE",
                  "status": "IN_PROGRESS",
                  "clientCompany": "롯데",
                  "budget": 5000000,
                  "paidAt": null,
                  "paidDate": "2026-07-15",
                  "paidTime": null,
                  "paidText": "7월 15일",
                  "paidPrecision": "DATE_ONLY",
                  "confidence": 0.94,
                  "warnings": []
                }
                판단 기준: 전체 예산은 budget에 넣습니다. 지급 예정일은 paidDate에 넣습니다. 계약금처럼 예산과 별도의 지급 세부 금액은 DTO에 별도 필드가 없다면 memo에 남깁니다. 회의 날짜와 지급 날짜를 혼동하지 않습니다.

                8. 장소가 회사명처럼 보일 수 있는 문장
                입력: "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의"
                기대 결과:
                {
                  "sourceText": "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의",
                  "categoryId": null,
                  "title": "팀 회의",
                  "content": "팀 회의",
                  "memo": "SSAFY 1층 회의실",
                  "startAt": "2026-06-11T15:00:00",
                  "startDate": "2026-06-11",
                  "startTime": "15:00:00",
                  "startText": "내일 오후 3시",
                  "startPrecision": "DATE_TIME",
                  "endAt": null,
                  "endDate": null,
                  "endTime": null,
                  "endText": null,
                  "endPrecision": "NONE",
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": null,
                  "paidAt": null,
                  "paidDate": null,
                  "paidTime": null,
                  "paidText": null,
                  "paidPrecision": "NONE",
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
                  "startAt": "2026-06-20T16:00:00",
                  "startDate": "2026-06-20",
                  "startTime": "16:00:00",
                  "startText": "6월 20일 오후 4시",
                  "startPrecision": "DATE_TIME",
                  "endAt": null,
                  "endDate": null,
                  "endTime": null,
                  "endText": null,
                  "endPrecision": "NONE",
                  "status": "IN_PROGRESS",
                  "clientCompany": null,
                  "budget": null,
                  "paidAt": null,
                  "paidDate": null,
                  "paidTime": null,
                  "paidText": null,
                  "paidPrecision": "NONE",
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
                  "startDate": string|null,
                  "startTime": string|null,
                  "startText": string|null,
                  "startPrecision": "NONE"|"DATE_ONLY"|"TIME_ONLY"|"DATE_TIME",
                  "endAt": string|null,
                  "endDate": string|null,
                  "endTime": string|null,
                  "endText": string|null,
                  "endPrecision": "NONE"|"DATE_ONLY"|"TIME_ONLY"|"DATE_TIME",
                  "status": "IN_PROGRESS"|"COMPLETED"|"CANCELLED"|null,
                  "clientCompany": string|null,
                  "budget": number|null,
                  "budgetAmount": number|null,
                  "depositAmount": number|null,
                  "paidAmount": number|null,
                  "balanceAmount": number|null,
                  "contractAmount": number|null,
                  "budgetText": string|null,
                  "paidAt": string|null,
                  "paidDate": string|null,
                  "paidTime": string|null,
                  "paidText": string|null,
                  "paidPrecision": "NONE"|"DATE_ONLY"|"TIME_ONLY"|"DATE_TIME",
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
                설명 문장, 마크다운, 코드블록은 출력하지 마세요.
                첫 글자는 { 이고 마지막 글자는 } 이어야 합니다.
                content는 null이 아닌 문자열이어야 합니다.
                이전 응답 검증 실패 사유: %s

                %s
                """.formatted(failureReason, createPrompt(sourceText));
    }

    private String extractResponsesText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String outputText = extractOutputArrayText(root);
        if (StringUtils.hasText(outputText)) {
            return outputText;
        }

        JsonNode outputTextNode = root.path("output_text");
        if (outputTextNode.isTextual() && StringUtils.hasText(outputTextNode.asString())) {
            return outputTextNode.asString();
        }

        throw new AiResponseValidationException("GMS AI response text is empty");
    }

    private String extractOutputArrayText(JsonNode root) {
        JsonNode outputNode = root.path("output");
        if (!outputNode.isArray()) {
            return null;
        }

        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode outputItem : outputNode) {
            appendTextContent(textBuilder, outputItem.path("content"));
            appendTextNode(textBuilder, outputItem.path("text"));
        }
        return textBuilder.toString();
    }

    private void appendTextContent(StringBuilder textBuilder, JsonNode contentNode) {
        if (contentNode.isArray()) {
            for (JsonNode contentItem : contentNode) {
                appendTextNode(textBuilder, contentItem.path("text"));
            }
            return;
        }

        appendTextNode(textBuilder, contentNode);
    }

    private void appendTextNode(StringBuilder textBuilder, JsonNode textNode) {
        if (!textNode.isTextual() || !StringUtils.hasText(textNode.asString())) {
            return;
        }
        if (textBuilder.length() > 0) {
            textBuilder.append("\n");
        }
        textBuilder.append(textNode.asString());
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

    private static RestClient createTimeoutRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MILLIS));
        requestFactory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MILLIS));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private record GmsAiConfig(String apiKey, String url, String model) {

        private boolean apiKeyPresent() {
            return isConfigured(apiKey);
        }

        private boolean urlPresent() {
            return isConfigured(url);
        }

        private boolean modelPresent() {
            return isConfigured(model);
        }

        private void validate() {
            if (!apiKeyPresent()) {
                throw new IllegalStateException("GMS API key is not configured");
            }
            if (!urlPresent()) {
                throw new IllegalStateException("GMS AI URL is not configured");
            }
            if (!modelPresent()) {
                throw new IllegalStateException("GMS AI model is not configured");
            }
        }
    }

    private static class AiResponseValidationException extends Exception {

        private AiResponseValidationException(String message) {
            super(message);
        }

        private AiResponseValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
