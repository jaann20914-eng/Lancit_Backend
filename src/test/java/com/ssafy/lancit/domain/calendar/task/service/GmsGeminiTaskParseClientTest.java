package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
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

    private static final String GEMINI_URL = "https://example.test/gemini";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseRetriesOnceWhenFirstGeminiResponseMissesRequiredFields() throws Exception {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GmsGeminiTaskParseClient client = new GmsGeminiTaskParseClient(
                restClientBuilder.build(),
                objectMapper,
                new MockEnvironment()
                        .withProperty("gms.provider", "gemini")
                        .withProperty("gms.api.key", "test-api-key")
                        .withProperty("gms.gemini.url", GEMINI_URL)
        );

        server.expect(requestTo(GEMINI_URL))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andRespond(withSuccess(geminiResponse("""
                        {"title":"팀 회의"}
                        """), MediaType.APPLICATION_JSON));
        server.expect(requestTo(GEMINI_URL))
                .andExpect(content().string(containsString("이전 응답은 DTO 스키마에 맞지 않습니다")))
                .andRespond(withSuccess(geminiResponse("""
                        {
                          "sourceText": "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의",
                          "categoryId": null,
                          "title": "팀 회의",
                          "content": "팀 회의",
                          "memo": "SSAFY 1층 회의실",
                          "startAt": "2026-06-11T15:00:00",
                          "endAt": null,
                          "status": "IN_PROGRESS",
                          "clientCompany": null,
                          "budget": null,
                          "paidAt": null,
                          "confidence": 0.93,
                          "warnings": []
                        }
                        """), MediaType.APPLICATION_JSON));

        TaskParseResponseDTO result = client.parse("내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의");

        assertThat(result.getTitle()).isEqualTo("팀 회의");
        assertThat(result.getMemo()).isEqualTo("SSAFY 1층 회의실");
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getCategoryId()).isNull();
        assertThat(result.getStartAt()).isEqualTo(LocalDateTime.of(2026, 6, 11, 15, 0));
        server.verify();
    }

    private String geminiResponse(String text) throws Exception {
        return """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": %s
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(text.trim()));
    }
}
