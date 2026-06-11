package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.global.enums.DateTimePrecision;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaskParseGoldenSetEvaluationTest {

    private static final String GOLDEN_SET_RESOURCE = "/calendar-task-parse-golden-set.json";
    private static final List<String> DATE_TIME_GROUPS = List.of("start", "end", "paid");
    private static final List<String> AMOUNT_FIELDS = List.of(
            "budgetAmount",
            "depositAmount",
            "paidAmount",
            "balanceAmount",
            "contractAmount"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaskParseService taskParseService = new TaskParseService();

    @Test
    void evaluateGoldenSet() throws Exception {
        Assumptions.assumeTrue(
                Boolean.getBoolean("golden.eval"),
                "Golden set evaluation is opt-in. Run with -Dgolden.eval=true"
        );

        JsonNode testCases = loadGoldenSet();
        EvaluationSummary summary = new EvaluationSummary(testCases.size());

        for (JsonNode testCase : testCases) {
            GoldenCaseResult result = evaluateCase(testCase);
            summary.add(result);
        }

        String report = summary.toMarkdown();
        Path reportPath = Path.of("target", "calendar-task-parse-golden-report.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
        System.out.println(report);
        System.out.println("Golden set report written to " + reportPath.toAbsolutePath());

        double minimumScore = Double.parseDouble(System.getProperty("golden.minScore", "0.0"));
        assertThat(summary.score()).isGreaterThanOrEqualTo(minimumScore);
    }

    private JsonNode loadGoldenSet() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(GOLDEN_SET_RESOURCE)) {
            assertThat(inputStream)
                    .as("Golden set resource must exist: " + GOLDEN_SET_RESOURCE)
                    .isNotNull();
            JsonNode root = objectMapper.readTree(inputStream);
            assertThat(root.isArray()).isTrue();
            return root;
        }
    }

    private GoldenCaseResult evaluateCase(JsonNode testCase) {
        String id = text(testCase, "id");
        String category = text(testCase, "category");
        String input = text(testCase, "input");
        JsonNode expected = testCase.path("expected");
        TaskParseResponseDTO actual = taskParseService.parse(TaskParseRequestDTO.builder()
                .sourceText(input)
                .build());

        List<String> passes = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        compareTextContains("title", textOrNull(expected, "title"), actual.getTitle(), passes, failures);
        compareTextContains("memo", textOrNull(expected, "memo"), actual.getMemo(), passes, failures);
        compareStrict("clientCompany", textOrNull(expected, "clientCompany"), actual.getClientCompany(), passes, failures);
        compareIntegerAlias(expected, actual, passes, failures);
        compareDateTimeGroups(expected, actual, passes, failures);
        compareBudgetText(expected, actual, passes, failures);

        return new GoldenCaseResult(id, category, input, passes, failures);
    }

    private void compareDateTimeGroups(JsonNode expected,
                                       TaskParseResponseDTO actual,
                                       List<String> passes,
                                       List<String> failures) {
        for (String group : DATE_TIME_GROUPS) {
            boolean groupMentioned = hasAnyDateTimeExpectation(expected, group);
            if (!groupMentioned) {
                compareStrict(group + "At", null, actualAt(actual, group), passes, failures);
                compareStrict(group + "Date", null, actualDate(actual, group), passes, failures);
                compareStrict(group + "Time", null, actualTime(actual, group), passes, failures);
                compareStrict(group + "Precision", DateTimePrecision.NONE, actualPrecision(actual, group), passes, failures);
                continue;
            }

            if (expected.has(group + "At")) {
                compareStrict(group + "At", parseDateTimeOrNull(expected, group + "At"), actualAt(actual, group), passes, failures);
            }
            if (expected.has(group + "Date")) {
                compareStrict(group + "Date", parseDateOrNull(expected, group + "Date"), actualDate(actual, group), passes, failures);
                compareStrict(group + "At", null, actualAt(actual, group), passes, failures);
            }
            if (expected.has(group + "Time")) {
                compareStrict(group + "Time", parseTimeOrNull(expected, group + "Time"), actualTime(actual, group), passes, failures);
                compareStrict(group + "At", null, actualAt(actual, group), passes, failures);
            }
            if (expected.has(group + "Text")) {
                compareTextContains(group + "Text", textOrNull(expected, group + "Text"), actualText(actual, group), passes, failures);
            }
            if (expected.has(group + "Precision")) {
                compareStrict(group + "Precision", parsePrecision(expected, group + "Precision"), actualPrecision(actual, group), passes, failures);
            }
        }
    }

    private void compareIntegerAlias(JsonNode expected,
                                     TaskParseResponseDTO actual,
                                     List<String> passes,
                                     List<String> failures) {
        if (expected.has("budgetAmount")) {
            compareStrict("budgetAmount", expected.path("budgetAmount").intValue(), actual.getBudgetAmount(), passes, failures);
        }
        if (expected.has("depositAmount")) {
            compareStrict("depositAmount", expected.path("depositAmount").intValue(), actual.getDepositAmount(), passes, failures);
        }
        if (expected.has("paidAmount")) {
            compareStrict("paidAmount", expected.path("paidAmount").intValue(), actual.getPaidAmount(), passes, failures);
        }
        if (expected.has("balanceAmount")) {
            compareStrict("balanceAmount", expected.path("balanceAmount").intValue(), actual.getBalanceAmount(), passes, failures);
        }
        if (expected.has("contractAmount")) {
            compareStrict("contractAmount", expected.path("contractAmount").intValue(), actual.getContractAmount(), passes, failures);
        }
    }

    private void compareBudgetText(JsonNode expected,
                                   TaskParseResponseDTO actual,
                                   List<String> passes,
                                   List<String> failures) {
        if (expected.has("budgetText")) {
            compareTextContains("budgetText", textOrNull(expected, "budgetText"), actual.getBudgetText(), passes, failures);
        }
    }

    private boolean hasAnyDateTimeExpectation(JsonNode expected, String group) {
        return expected.has(group + "At")
                || expected.has(group + "Date")
                || expected.has(group + "Time")
                || expected.has(group + "Text")
                || expected.has(group + "Precision");
    }

    private void compareStrict(String fieldName,
                               Object expected,
                               Object actual,
                               List<String> passes,
                               List<String> failures) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            passes.add(fieldName);
            return;
        }
        failures.add(fieldName + " expected=" + expected + " actual=" + actual);
    }

    private void compareTextContains(String fieldName,
                                     String expected,
                                     String actual,
                                     List<String> passes,
                                     List<String> failures) {
        if (expected == null) {
            return;
        }
        if (actual != null && normalize(actual).contains(normalize(expected))) {
            passes.add(fieldName);
            return;
        }
        failures.add(fieldName + " expectedContains=\"" + expected + "\" actual=\"" + actual + "\"");
    }

    private String normalize(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asString();
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asString();
    }

    private LocalDateTime parseDateTimeOrNull(JsonNode node, String fieldName) {
        if (node.path(fieldName).isNull()) {
            return null;
        }
        return LocalDateTime.parse(text(node, fieldName));
    }

    private LocalDate parseDateOrNull(JsonNode node, String fieldName) {
        if (node.path(fieldName).isNull()) {
            return null;
        }
        return LocalDate.parse(text(node, fieldName));
    }

    private LocalTime parseTimeOrNull(JsonNode node, String fieldName) {
        if (node.path(fieldName).isNull()) {
            return null;
        }
        return LocalTime.parse(text(node, fieldName));
    }

    private DateTimePrecision parsePrecision(JsonNode node, String fieldName) {
        return DateTimePrecision.valueOf(text(node, fieldName));
    }

    private LocalDateTime actualAt(TaskParseResponseDTO actual, String group) {
        return switch (group) {
            case "start" -> actual.getStartAt();
            case "end" -> actual.getEndAt();
            case "paid" -> actual.getPaidAt();
            default -> throw new IllegalArgumentException("Unknown date-time group: " + group);
        };
    }

    private LocalDate actualDate(TaskParseResponseDTO actual, String group) {
        return switch (group) {
            case "start" -> actual.getStartDate();
            case "end" -> actual.getEndDate();
            case "paid" -> actual.getPaidDate();
            default -> throw new IllegalArgumentException("Unknown date-time group: " + group);
        };
    }

    private LocalTime actualTime(TaskParseResponseDTO actual, String group) {
        return switch (group) {
            case "start" -> actual.getStartTime();
            case "end" -> actual.getEndTime();
            case "paid" -> actual.getPaidTime();
            default -> throw new IllegalArgumentException("Unknown date-time group: " + group);
        };
    }

    private String actualText(TaskParseResponseDTO actual, String group) {
        return switch (group) {
            case "start" -> actual.getStartText();
            case "end" -> actual.getEndText();
            case "paid" -> actual.getPaidText();
            default -> throw new IllegalArgumentException("Unknown date-time group: " + group);
        };
    }

    private DateTimePrecision actualPrecision(TaskParseResponseDTO actual, String group) {
        return switch (group) {
            case "start" -> actual.getStartPrecision();
            case "end" -> actual.getEndPrecision();
            case "paid" -> actual.getPaidPrecision();
            default -> throw new IllegalArgumentException("Unknown date-time group: " + group);
        };
    }

    private record GoldenCaseResult(String id,
                                    String category,
                                    String input,
                                    List<String> passes,
                                    List<String> failures) {

        private boolean passed() {
            return failures.isEmpty();
        }

        private int totalChecks() {
            return passes.size() + failures.size();
        }

        private int passedChecks() {
            return passes.size();
        }
    }

    private static class EvaluationSummary {
        private final int totalCases;
        private final List<GoldenCaseResult> results = new ArrayList<>();

        private EvaluationSummary(int totalCases) {
            this.totalCases = totalCases;
        }

        private void add(GoldenCaseResult result) {
            results.add(result);
        }

        private double score() {
            int totalChecks = results.stream().mapToInt(GoldenCaseResult::totalChecks).sum();
            if (totalChecks == 0) {
                return 0.0;
            }
            int passedChecks = results.stream().mapToInt(GoldenCaseResult::passedChecks).sum();
            return passedChecks / (double) totalChecks;
        }

        private String toMarkdown() {
            long passedCases = results.stream().filter(GoldenCaseResult::passed).count();
            int totalChecks = results.stream().mapToInt(GoldenCaseResult::totalChecks).sum();
            int passedChecks = results.stream().mapToInt(GoldenCaseResult::passedChecks).sum();
            StringBuilder report = new StringBuilder();
            report.append("# Calendar Task Parse Golden Set Evaluation\n\n");
            report.append("- Total cases: ").append(totalCases).append("\n");
            report.append("- Passed cases: ").append(passedCases).append("/").append(totalCases).append("\n");
            report.append("- Passed checks: ").append(passedChecks).append("/").append(totalChecks).append("\n");
            report.append("- Score: ").append(String.format(Locale.ROOT, "%.2f%%", score() * 100)).append("\n\n");
            report.append("## Category Summary\n\n");
            report.append("| Category | Passed Cases | Total Cases |\n");
            report.append("| --- | ---: | ---: |\n");
            for (Map.Entry<String, CategorySummary> entry : categorySummaries().entrySet()) {
                CategorySummary categorySummary = entry.getValue();
                report.append("| ")
                        .append(entry.getKey())
                        .append(" | ")
                        .append(categorySummary.passedCases())
                        .append(" | ")
                        .append(categorySummary.totalCases())
                        .append(" |\n");
            }
            report.append("\n");
            report.append("## Failures\n\n");
            for (GoldenCaseResult result : results) {
                if (result.passed()) {
                    continue;
                }
                report.append("### ").append(result.id()).append(" - ").append(result.category()).append("\n");
                report.append("- Input: `").append(result.input()).append("`\n");
                for (String failure : result.failures()) {
                    report.append("- ").append(failure).append("\n");
                }
                report.append("\n");
            }
            return report.toString();
        }

        private Map<String, CategorySummary> categorySummaries() {
            Map<String, CategorySummary> summaries = new LinkedHashMap<>();
            for (GoldenCaseResult result : results) {
                summaries.computeIfAbsent(result.category(), ignored -> new CategorySummary())
                        .add(result.passed());
            }
            return summaries;
        }
    }

    private static class CategorySummary {
        private int totalCases;
        private int passedCases;

        private void add(boolean passed) {
            totalCases++;
            if (passed) {
                passedCases++;
            }
        }

        private int totalCases() {
            return totalCases;
        }

        private int passedCases() {
            return passedCases;
        }
    }
}
