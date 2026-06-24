package com.ssafy.lancit.domain.calendar.task.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiTaskParseClientTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2026, 6, 24).atStartOfDay(SEOUL_ZONE).toInstant(),
            SEOUL_ZONE
    );

    @Test
    @SuppressWarnings("unchecked")
    void createRequestBodyUsesGenerateContentStructuredOutput() {
        GeminiTaskParseClient client = new GeminiTaskParseClient(
                new ObjectMapper(),
                new MockEnvironment(),
                FIXED_CLOCK
        );

        Map<String, Object> requestBody = client.createRequestBody("프로젝트 검토 일정");

        assertThat(requestBody).containsOnlyKeys("contents", "generationConfig");

        List<Map<String, Object>> contents = (List<Map<String, Object>>) requestBody.get("contents");
        assertThat(contents).hasSize(1);
        assertThat(contents.get(0)).containsEntry("role", "user");
        List<Map<String, String>> parts = (List<Map<String, String>>) contents.get(0).get("parts");
        assertThat(parts).hasSize(1);
        assertThat(parts.get(0)).containsEntry("text", "프로젝트 검토 일정");

        Map<String, Object> generationConfig = (Map<String, Object>) requestBody.get("generationConfig");
        assertThat(generationConfig).containsEntry("responseMimeType", "application/json");

        Map<String, Object> responseSchema = (Map<String, Object>) generationConfig.get("responseSchema");
        assertThat(responseSchema).containsEntry("type", "OBJECT");
        assertThat(responseSchema).containsKeys("properties", "required");

        Map<String, Object> properties = (Map<String, Object>) responseSchema.get("properties");
        assertThat(properties).containsKeys("sourceText", "categoryId", "title", "warnings");
        assertThat((Map<String, Object>) properties.get("title"))
                .containsEntry("type", "STRING")
                .containsEntry("nullable", true);
        assertThat((Map<String, Object>) properties.get("content"))
                .containsEntry("type", "STRING")
                .containsEntry("nullable", true);

        List<String> requiredFields = (List<String>) responseSchema.get("required");
        assertThat(requiredFields).contains("sourceText", "categoryId", "title", "content", "warnings");
    }

    @Test
    void createPromptDocumentsContentAndMemoPolicy() {
        GeminiTaskParseClient client = new GeminiTaskParseClient(
                new ObjectMapper(),
                new MockEnvironment(),
                FIXED_CLOCK
        );

        String prompt = client.createPrompt("2026년 7월 12일 14:00~16:00 회의");

        assertThat(prompt)
                .contains("content는 일정 설명, 목적, 안건, 논의 내용, 작업 내용, 진행 내용")
                .contains("content=null")
                .contains("면접/채용/인터뷰/면담 같은 개인 채용 일정의 회사명")
                .contains("\"sourceText\": \"2026년 7월 12일 14:00~16:00 회의\"")
                .contains("\"title\": \"회의\"")
                .contains("\"content\": null")
                .contains("\"sourceText\": \"삼성전자 면접 6월 25일 오후 3시\"")
                .contains("\"title\": \"삼성전자 면접\"")
                .contains("\"clientCompany\": null")
                .contains("\"memo\": \"온라인: Zoom\"")
                .doesNotContain("content는 null이 아닌 한 문장 요약");
    }

    @Test
    void extractGenerateContentTextReadsCandidatePartText() throws Exception {
        GeminiTaskParseClient client = new GeminiTaskParseClient(
                new ObjectMapper(),
                new MockEnvironment(),
                FIXED_CLOCK
        );

        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"sourceText\\":\\"프로젝트 검토 일정\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        assertThat(client.extractGenerateContentText(responseBody))
                .isEqualTo("{\"sourceText\":\"프로젝트 검토 일정\"}");
    }

    @Test
    void missingApiKeyStopsBeforeExternalRequestPath() {
        GeminiTaskParseClient client = new GeminiTaskParseClient(
                new ObjectMapper(),
                new MockEnvironment(),
                FIXED_CLOCK
        );

        assertThatThrownBy(() -> client.parse("프로젝트 검토 일정"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gemini API key is not configured");
    }
}
