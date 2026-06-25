package com.ssafy.lancit.domain.externaljob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class GeminiExternalJobPersonalRecommendationClientTest {

    @Test
    @DisplayName("Gemini 개인화 추천 점수는 추천 타입별 구간으로 정규화한다")
    void parseRecommendations_normalizesScoreByRecommendationType() throws Exception {
        List<ExternalJobPersonalRecommendation> recommendations = parseRecommendations("""
                {
                  "recommendations": [
                    {
                      "externalJobId": 1,
                      "recommendationType": "HIGHLY_RECOMMENDED",
                      "recommendationScore": 75
                    },
                    {
                      "externalJobId": 2,
                      "recommendationType": "RECOMMENDED",
                      "recommendationScore": 95
                    },
                    {
                      "externalJobId": 3,
                      "recommendationType": "POSSIBLE",
                      "recommendationScore": 20
                    },
                    {
                      "externalJobId": 4,
                      "recommendationType": "EXCLUDED",
                      "recommendationScore": 80
                    },
                    {
                      "externalJobId": 5,
                      "recommendationScore": 85
                    }
                  ]
                }
                """);

        assertThat(recommendations)
                .extracting(ExternalJobPersonalRecommendation::getRecommendationType,
                        ExternalJobPersonalRecommendation::getRecommendationScore)
                .containsExactly(
                        tuple(ExternalJobRecommendationType.HIGHLY_RECOMMENDED, 80),
                        tuple(ExternalJobRecommendationType.RECOMMENDED, 79),
                        tuple(ExternalJobRecommendationType.POSSIBLE, 40),
                        tuple(ExternalJobRecommendationType.EXCLUDED, 39),
                        tuple(ExternalJobRecommendationType.HIGHLY_RECOMMENDED, 85)
                );
    }

    @SuppressWarnings("unchecked")
    private List<ExternalJobPersonalRecommendation> parseRecommendations(String aiText) throws Exception {
        GeminiExternalJobPersonalRecommendationClient client =
                new GeminiExternalJobPersonalRecommendationClient(new ObjectMapper(), null);
        Method method = GeminiExternalJobPersonalRecommendationClient.class
                .getDeclaredMethod("parseRecommendations", String.class);
        method.setAccessible(true);
        return (List<ExternalJobPersonalRecommendation>) method.invoke(client, aiText);
    }
}
