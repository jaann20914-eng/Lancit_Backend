package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiTaskParseContractTest {

    private static final String AI_CONTRACT_SET_RESOURCE = "/calendar-task-parse-ai-contract-set.json";
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2026, 6, 24).atStartOfDay(SEOUL_ZONE).toInstant(),
            SEOUL_ZONE
    );
    private static final Set<Integer> TRANSIENT_EXTERNAL_HTTP_STATUSES = Set.of(429, 500, 502, 503, 504);
    private static final Set<String> EXACT_VALUE_FIELDS = Set.of(
            "startAt",
            "startDate",
            "startTime",
            "startPrecision",
            "endAt",
            "endDate",
            "endTime",
            "endPrecision",
            "paidAt",
            "paidDate",
            "paidTime",
            "paidPrecision",
            "budget",
            "budgetAmount",
            "depositAmount",
            "paidAmount",
            "balanceAmount",
            "contractAmount"
    );
    private static final Set<String> DTO_FIELDS = Set.of(
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
            "requiresConfirmation",
            "confidence",
            "warnings"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void activeContractSetCasesFollowGeminiDtoContract() throws Exception {
        String apiKey = firstText(System.getProperty("gemini.api.key"), System.getenv("GEMINI_API_KEY"));
        Assumptions.assumeTrue(hasText(apiKey), "GEMINI_API_KEY is not configured");

        GeminiTaskParseClient client = new GeminiTaskParseClient(
                objectMapper,
                geminiEnvironment(apiKey),
                FIXED_CLOCK
        );
        JsonNode testCases = loadJsonArray(AI_CONTRACT_SET_RESOURCE);
        int activeCount = 0;

        for (JsonNode testCase : testCases) {
            if (!testCase.path("active").asBoolean(false)) {
                continue;
            }
            activeCount++;
            assertAiContractCase(client, testCase);
        }

        assertThat(activeCount).isPositive();
    }

    private void assertAiContractCase(GeminiTaskParseClient client, JsonNode testCase) throws Exception {
        String id = testCase.path("id").asString();
        String sourceText = testCase.path("sourceText").asString();
        JsonNode expected = testCase.path("expected");

        TaskParseResponseDTO actual = parseOrSkipTransientExternalFailure(client, id, sourceText);
        JsonNode actualNode = objectMapper.valueToTree(actual);

        assertThat(actual.getSourceText()).as(id + " sourceText").isEqualTo(sourceText);
        assertRequiredFields(id, actualNode, expected.path("requiredFields"));
        assertNullableFields(id, actualNode, expected.path("nullableFields"));
        assertTitleContains(id, actual.getTitle(), expected.path("titleContains"));
        assertTitleContainsAny(id, actual.getTitle(), expected.path("titleContainsAny"));
        assertRequiredKeywords(id, actualNode, expected.path("requiredKeywords"));
        assertExactValues(id, actualNode, expected);
    }

    private TaskParseResponseDTO parseOrSkipTransientExternalFailure(GeminiTaskParseClient client,
                                                                     String id,
                                                                     String sourceText) {
        try {
            return client.parse(sourceText);
        } catch (RuntimeException e) {
            RestClientResponseException httpException = findHttpResponseException(e);
            if (httpException == null) {
                throw e;
            }

            int statusCode = httpException.getStatusCode().value();
            if (!TRANSIENT_EXTERNAL_HTTP_STATUSES.contains(statusCode)) {
                throw e;
            }

            String statusText = hasText(httpException.getStatusText()) ? " " + httpException.getStatusText() : "";
            throw new TestAbortedException(
                    "Skipping Gemini contract case " + id
                            + ": HTTP " + statusCode + statusText
                            + " from external Gemini service",
                    e
            );
        }
    }

    private RestClientResponseException findHttpResponseException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException httpException) {
                return httpException;
            }
            current = current.getCause();
        }
        return null;
    }

    private void assertRequiredFields(String id, JsonNode actualNode, JsonNode requiredFields) {
        for (JsonNode fieldNode : requiredFields) {
            String field = fieldNode.asString();
            JsonNode actualValue = actualNode.path(field);
            assertThat(actualValue.isMissingNode() || actualValue.isNull())
                    .as(id + " required field must be present and non-null: " + field)
                    .isFalse();
            if (actualValue.isTextual()) {
                assertThat(actualValue.asString())
                        .as(id + " required text field must not be blank: " + field)
                        .isNotBlank();
            }
        }
    }

    private void assertNullableFields(String id, JsonNode actualNode, JsonNode nullableFieldsNode) {
        List<String> nullableFields = stringList(nullableFieldsNode);
        List<String> unexpectedNullFields = new ArrayList<>();
        for (String field : DTO_FIELDS) {
            JsonNode actualValue = actualNode.path(field);
            if ((actualValue.isMissingNode() || actualValue.isNull()) && !nullableFields.contains(field)) {
                unexpectedNullFields.add(field);
            }
        }

        assertThat(unexpectedNullFields)
                .as(id + " null fields must be declared in nullableFields")
                .isEmpty();
    }

    private void assertTitleContains(String id, String actualTitle, JsonNode titleContains) {
        assertThat(actualTitle)
                .as(id + " title")
                .isNotBlank();
        for (JsonNode tokenNode : titleContains) {
            String token = tokenNode.asString();
            assertThat(normalize(actualTitle))
                    .as(id + " title should contain: " + token)
                    .contains(normalize(token));
        }
    }

    private void assertTitleContainsAny(String id, String actualTitle, JsonNode titleContainsAny) {
        if (titleContainsAny.isMissingNode() || titleContainsAny.isEmpty()) {
            return;
        }

        List<String> tokens = stringList(titleContainsAny);
        assertThat(tokens)
                .as(id + " titleContainsAny")
                .isNotEmpty();
        assertThat(tokens.stream().anyMatch(token -> normalize(actualTitle).contains(normalize(token))))
                .as(id + " title should contain at least one of: " + tokens)
                .isTrue();
    }

    private void assertRequiredKeywords(String id, JsonNode actualNode, JsonNode requiredKeywords) {
        for (Map.Entry<String, JsonNode> entry : requiredKeywords.properties()) {
            String field = entry.getKey();
            String actualText = actualNode.path(field).isTextual() ? actualNode.path(field).asString() : null;

            assertThat(actualText)
                    .as(id + " keyword field must be textual: " + field)
                    .isNotBlank();
            for (JsonNode keywordNode : entry.getValue()) {
                String keyword = keywordNode.asString();
                assertThat(normalize(actualText))
                        .as(id + " " + field + " should contain keyword: " + keyword)
                        .contains(normalize(keyword));
            }
        }
    }

    private void assertExactValues(String id, JsonNode actualNode, JsonNode expected) {
        for (Map.Entry<String, JsonNode> entry : expected.properties()) {
            String field = entry.getKey();
            if (!EXACT_VALUE_FIELDS.contains(field)) {
                continue;
            }

            JsonNode expectedValue = entry.getValue();
            JsonNode actualValue = actualNode.path(field);
            assertThat(actualValue.isMissingNode() || actualValue.isNull())
                    .as(id + " exact field must be present and non-null: " + field)
                    .isFalse();
            assertExactValue(id, field, actualValue, expectedValue);
        }
    }

    private void assertExactValue(String id, String field, JsonNode actualValue, JsonNode expectedValue) {
        if (field.endsWith("At")) {
            assertThat(LocalDateTime.parse(actualValue.asString()))
                    .as(id + " " + field)
                    .isEqualTo(LocalDateTime.parse(expectedValue.asString()));
            return;
        }
        if (field.endsWith("Date")) {
            assertThat(LocalDate.parse(actualValue.asString()))
                    .as(id + " " + field)
                    .isEqualTo(LocalDate.parse(expectedValue.asString()));
            return;
        }
        if (field.endsWith("Time")) {
            assertThat(LocalTime.parse(actualValue.asString()))
                    .as(id + " " + field)
                    .isEqualTo(LocalTime.parse(expectedValue.asString()));
            return;
        }
        if (expectedValue.isIntegralNumber()) {
            assertThat(actualValue.asInt())
                    .as(id + " " + field)
                    .isEqualTo(expectedValue.asInt());
            return;
        }

        assertThat(actualValue.asString())
                .as(id + " " + field)
                .isEqualTo(expectedValue.asString());
    }

    private JsonNode loadJsonArray(String resourceName) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resourceName)) {
            assertThat(inputStream)
                    .as("Test resource must exist: " + resourceName)
                    .isNotNull();
            JsonNode root = objectMapper.readTree(inputStream);
            assertThat(root.isArray()).isTrue();
            return root;
        }
    }

    private MockEnvironment geminiEnvironment(String apiKey) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("gemini.api.key", apiKey);

        String baseUrl = firstText(System.getProperty("gemini.api.base-url"), System.getenv("GEMINI_API_BASE_URL"));
        if (hasText(baseUrl)) {
            environment.withProperty("gemini.api.base-url", baseUrl);
        }

        String model = firstText(System.getProperty("gemini.model"), System.getenv("GEMINI_MODEL"));
        if (hasText(model)) {
            environment.withProperty("gemini.model", model);
        }
        return environment;
    }

    private List<String> stringList(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            values.add(node.asString());
        }
        return values;
    }

    private String normalize(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
