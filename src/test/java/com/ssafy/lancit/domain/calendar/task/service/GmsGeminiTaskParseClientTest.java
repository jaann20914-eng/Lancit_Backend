package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.global.enums.DateTimePrecision;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GmsGeminiTaskParseClientTest {

    private static final String AI_URL = "https://example.test/responses";
    private static final String AI_MODEL = "gpt-test-model";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseRetriesOnceWhenFirstResponsesApiResponseMissesRequiredFields() throws Exception {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GmsGeminiTaskParseClient client = new GmsGeminiTaskParseClient(
                restClientBuilder.build(),
                objectMapper,
                new MockEnvironment()
                        .withProperty("gms.api.key", "test-api-key")
                        .withProperty("gms.ai.url", AI_URL)
                        .withProperty("gms.ai.model", AI_MODEL)
        );

        server.expect(requestTo(AI_URL))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().string(containsString("\"model\":\"" + AI_MODEL + "\"")))
                .andExpect(content().string(containsString("\"input\"")))
                .andExpect(content().string(containsString("사용자 원문")))
                .andRespond(withSuccess(responsesOutputTextResponse("""
                        {"title":"팀 회의"}
                        """), MediaType.APPLICATION_JSON));
        server.expect(requestTo(AI_URL))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().string(containsString("이전 응답은 DTO 스키마에 맞지 않습니다")))
                .andRespond(withSuccess(responsesOutputTextResponse("""
                        ```json
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
                          "budgetAmount": null,
                          "depositAmount": null,
                          "paidAmount": null,
                          "balanceAmount": null,
                          "contractAmount": null,
                          "budgetText": null,
                          "paidAt": null,
                          "paidDate": null,
                          "paidTime": null,
                          "paidText": null,
                          "paidPrecision": "NONE",
                          "confidence": 0.93,
                          "warnings": []
                        }
                        ```
                        """), MediaType.APPLICATION_JSON));

        TaskParseResponseDTO result = client.parse("내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의");

        assertThat(result.getTitle()).isEqualTo("팀 회의");
        assertThat(result.getMemo()).isEqualTo("SSAFY 1층 회의실");
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getCategoryId()).isNull();
        assertThat(result.getStartAt()).isEqualTo(LocalDateTime.of(2026, 6, 11, 15, 0));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        server.verify();
    }

    @Test
    void parseSupportsRootOutputTextField() throws Exception {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GmsGeminiTaskParseClient client = new GmsGeminiTaskParseClient(
                restClientBuilder.build(),
                objectMapper,
                new MockEnvironment()
                        .withProperty("gms.api.key", "test-api-key")
                        .withProperty("gms.ai.url", AI_URL)
                        .withProperty("gms.ai.model", AI_MODEL)
        );

        server.expect(requestTo(AI_URL))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andRespond(withSuccess(responsesRootOutputTextResponse("""
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
                          "budgetAmount": null,
                          "depositAmount": null,
                          "paidAmount": null,
                          "balanceAmount": null,
                          "contractAmount": null,
                          "budgetText": null,
                          "paidAt": null,
                          "paidDate": null,
                          "paidTime": null,
                          "paidText": null,
                          "paidPrecision": "NONE",
                          "confidence": 0.93,
                          "warnings": []
                        }
                        """), MediaType.APPLICATION_JSON));

        TaskParseResponseDTO result = client.parse("내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의");

        assertThat(result.getTitle()).isEqualTo("팀 회의");
        assertThat(result.getContent()).isEqualTo("팀 회의");
        server.verify();
    }

    private String responsesOutputTextResponse(String text) throws Exception {
        return """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": %s
                        }
                      ]
                    }
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(text.trim()));
    }

    private String responsesRootOutputTextResponse(String text) throws Exception {
        return """
                {
                  "output": [],
                  "output_text": %s
                }
                """.formatted(objectMapper.writeValueAsString(text.trim()));
    }
}
