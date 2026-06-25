package com.ssafy.lancit.domain.externaljob.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCategoryRecommendationCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobImportFailedRow;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobImportResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalJobCsvImportService {

    private static final String IMPORT_TYPE_JOB = "EXTERNAL_JOB";
    private static final String IMPORT_TYPE_RECOMMENDATION = "CATEGORY_RECOMMENDATION";
    private static final String DEFAULT_MATCHED_BY = "LLM_PRECOMPUTED";

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    );

    private final ExternalJobMapper externalJobMapper;
    private final ObjectMapper objectMapper;

    public ExternalJobImportResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<Map<String, String>> rows = readCsvRows(file);
        if (rows.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Map<String, String> firstRow = rows.get(0);
        if (isRecommendationCsv(firstRow)) {
            return importRecommendations(rows);
        }
        if (isExternalJobCsv(firstRow)) {
            return importExternalJobs(rows);
        }
        throw new CustomException(ErrorCode.INVALID_INPUT);
    }

    private ExternalJobImportResponse importExternalJobs(List<Map<String, String>> rows) {
        ImportAccumulator accumulator = new ImportAccumulator(IMPORT_TYPE_JOB);
        LocalDateTime importedAt = LocalDateTime.now();

        for (int index = 0; index < rows.size(); index++) {
            int rowNumber = index + 2;
            Map<String, String> row = rows.get(index);
            try {
                ExternalJobSource source = parseSource(firstValue(row, "source"), ExternalJobSource.SEOUL);
                String sourceJobId = required(firstValue(row, "source_job_id", "sourceJobId"));
                boolean existed = externalJobMapper.findBySourceAndSourceJobId(source, sourceJobId) != null;
                ExternalJobUpsertCommand command = toExternalJobCommand(row, source, sourceJobId, importedAt);
                int affectedRows = externalJobMapper.upsertExternalJob(command);
                accumulator.recordWrite(affectedRows, existed);
            } catch (RuntimeException e) {
                accumulator.fail(rowNumber,
                        firstValue(row, "source"),
                        firstValue(row, "source_job_id", "sourceJobId"),
                        failureReason(e));
            }
        }
        return accumulator.toResponse();
    }

    private ExternalJobImportResponse importRecommendations(List<Map<String, String>> rows) {
        ImportAccumulator accumulator = new ImportAccumulator(IMPORT_TYPE_RECOMMENDATION);

        for (int index = 0; index < rows.size(); index++) {
            int rowNumber = index + 2;
            Map<String, String> row = rows.get(index);
            ExternalJobSource source = null;
            String sourceJobId = null;
            try {
                String jobCategory = required(firstValue(row, "job_category", "jobCategory"));
                source = parseSource(firstValue(row, "source"), ExternalJobSource.SEOUL);
                sourceJobId = required(firstValue(row, "source_job_id", "sourceJobId"));
                ExternalJobDTO externalJob = externalJobMapper.findBySourceAndSourceJobId(source, sourceJobId);
                if (externalJob == null || externalJob.getId() == null) {
                    accumulator.fail(rowNumber, source.name(), sourceJobId, "EXTERNAL_JOB_NOT_FOUND");
                    continue;
                }

                ExternalJobRecommendationType recommendationType = parseEnum(
                        ExternalJobRecommendationType.class,
                        firstValue(row, "recommendation_type", "recommendationType"),
                        null);
                Integer rawScore = parseInteger(firstValue(row, "recommendation_score", "recommendationScore"));
                if (recommendationType == null) {
                    recommendationType = recommendationTypeForScore(rawScore == null ? 0 : rawScore);
                }
                int recommendationScore = normalizeRecommendationScore(rawScore, recommendationType);

                int affectedRows = externalJobMapper.upsertExternalJobCategoryRecommendation(
                        ExternalJobCategoryRecommendationCommand.builder()
                                .jobCategory(jobCategory)
                                .externalJobId(externalJob.getId())
                                .recommendationType(recommendationType)
                                .recommendationScore(recommendationScore)
                                .matchedBy(defaultIfBlank(firstValue(row, "matched_by", "matchedBy"), DEFAULT_MATCHED_BY))
                                .reason(trimToNull(firstValue(row, "reason")))
                                .build());
                accumulator.recordWrite(affectedRows, false);
            } catch (RuntimeException e) {
                accumulator.fail(rowNumber,
                        source == null ? firstValue(row, "source") : source.name(),
                        sourceJobId == null ? firstValue(row, "source_job_id", "sourceJobId") : sourceJobId,
                        failureReason(e));
            }
        }
        return accumulator.toResponse();
    }

    private ExternalJobUpsertCommand toExternalJobCommand(Map<String, String> row,
                                                          ExternalJobSource source,
                                                          String sourceJobId,
                                                          LocalDateTime importedAt) {
        ExternalFreelanceType freelanceType = parseEnum(
                ExternalFreelanceType.class,
                firstValue(row, "freelance_type", "freelanceType"),
                ExternalFreelanceType.UNKNOWN);
        ExternalJobRecommendationType recommendationType = parseEnum(
                ExternalJobRecommendationType.class,
                firstValue(row, "recommendation_type", "recommendationType"),
                ExternalJobRecommendationType.POSSIBLE);
        int recommendationScore = normalizeRecommendationScore(
                parseInteger(firstValue(row, "recommendation_score", "recommendationScore")),
                recommendationType);
        boolean visible = parseBoolean(firstValue(row, "is_visible", "visible"), true)
                && !ExternalFreelanceType.NOT_FREELANCE.equals(freelanceType);

        String originalPayloadJson = toJson(row);
        return ExternalJobUpsertCommand.builder()
                .source(source)
                .sourceJobId(sourceJobId)
                .sourceUrl(trimToNull(firstValue(row, "original_url", "source_url", "sourceUrl")))
                .title(defaultIfBlank(firstValue(row, "title"), sourceJobId))
                .companyName(trimToNull(firstValue(row, "company_name", "companyName")))
                .location(trimToNull(firstValue(row, "location")))
                .jobCategoryRaw(trimToNull(firstValue(row, "job_category", "jobCategory", "job_category_raw")))
                .employmentTypeRaw(trimToNull(firstValue(row, "employment_type", "employment_type_raw")))
                .salaryRaw(trimToNull(firstValue(row, "wage", "salary", "salary_raw")))
                .postedAt(parseDateTime(firstValue(row, "posted_at", "postedAt")))
                .deadlineAt(parseDateTime(firstValue(row, "deadline", "deadline_at", "deadlineAt")))
                .description(trimToNull(firstValue(row, "description")))
                .originalPayloadJson(originalPayloadJson)
                .payloadHash(sha256(originalPayloadJson))
                .freelanceType(freelanceType)
                .recommendationType(recommendationType)
                .recommendationScore(recommendationScore)
                .visible(visible)
                .visibilityReason(visible ? "VISIBLE" : "NOT_FREELANCE_OR_IMPORT_HIDDEN")
                .collectedAt(parseDateTime(firstValue(row, "collected_at", "collectedAt"), importedAt))
                .updatedAt(importedAt)
                .build();
    }

    private List<Map<String, String>> readCsvRows(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<List<String>> records = parseCsv(content);
            if (records.isEmpty()) {
                return List.of();
            }

            List<String> headers = records.get(0).stream()
                    .map(ExternalJobCsvImportService::normalizeHeader)
                    .toList();
            List<Map<String, String>> rows = new ArrayList<>();
            for (int index = 1; index < records.size(); index++) {
                List<String> record = records.get(index);
                if (isBlankRecord(record)) {
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (int column = 0; column < headers.size(); column++) {
                    String header = headers.get(column);
                    if (!StringUtils.hasText(header)) {
                        continue;
                    }
                    row.put(header, column < record.size() ? record.get(column).trim() : "");
                }
                rows.add(row);
            }
            return rows;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private static List<List<String>> parseCsv(String content) {
        String normalized = content == null ? "" : content.replace("\uFEFF", "");
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < normalized.length(); index++) {
            char ch = normalized.charAt(index);
            if (inQuotes) {
                if (ch == '"') {
                    if (index + 1 < normalized.length() && normalized.charAt(index + 1) == '"') {
                        cell.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
                continue;
            }

            if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                record.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n') {
                record.add(cell.toString());
                records.add(record);
                record = new ArrayList<>();
                cell.setLength(0);
            } else if (ch != '\r') {
                cell.append(ch);
            }
        }

        record.add(cell.toString());
        if (!isBlankRecord(record)) {
            records.add(record);
        }
        return records;
    }

    private boolean isExternalJobCsv(Map<String, String> firstRow) {
        return hasAnyKey(firstRow, "source_job_id", "sourceJobId") && hasAnyKey(firstRow, "title");
    }

    private boolean isRecommendationCsv(Map<String, String> firstRow) {
        return hasAnyKey(firstRow, "job_category", "jobCategory")
                && hasAnyKey(firstRow, "source_job_id", "sourceJobId")
                && hasAnyKey(firstRow, "recommendation_score", "recommendationScore");
    }

    private boolean hasAnyKey(Map<String, String> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(normalizeHeader(key))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlankRecord(List<String> record) {
        return record == null || record.stream().allMatch(value -> !StringUtils.hasText(value));
    }

    private static String normalizeHeader(String header) {
        String normalized = trimToNull(header);
        if (normalized == null) {
            return "";
        }
        return normalized
                .replace("\uFEFF", "")
                .replace('-', '_')
                .replace(' ', '_')
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String firstValue(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(normalizeHeader(key));
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static String required(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException("REQUIRED_FIELD_MISSING");
        }
        return trimmed;
    }

    private static ExternalJobSource parseSource(String value, ExternalJobSource defaultValue) {
        return parseEnum(ExternalJobSource.class, value, defaultValue);
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, T defaultValue) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("UNSUPPORTED_" + enumType.getSimpleName().toUpperCase(Locale.ROOT));
        }
    }

    private static Integer parseInteger(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("INVALID_NUMBER");
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return defaultValue;
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "y".equals(normalized)
                || "yes".equals(normalized);
    }

    private static LocalDateTime parseDateTime(String value) {
        return parseDateTime(value, null);
    }

    private static LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return defaultValue;
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("INVALID_DATE");
    }

    private static ExternalJobRecommendationType recommendationTypeForScore(int score) {
        if (score >= 80) {
            return ExternalJobRecommendationType.HIGHLY_RECOMMENDED;
        }
        if (score >= 60) {
            return ExternalJobRecommendationType.RECOMMENDED;
        }
        if (score >= 40) {
            return ExternalJobRecommendationType.POSSIBLE;
        }
        return ExternalJobRecommendationType.EXCLUDED;
    }

    private static int normalizeRecommendationScore(Integer score, ExternalJobRecommendationType type) {
        int candidate = score == null ? defaultScore(type) : Math.max(0, Math.min(100, score));
        return switch (type) {
            case HIGHLY_RECOMMENDED -> Math.max(80, candidate);
            case RECOMMENDED -> Math.max(60, Math.min(79, candidate));
            case POSSIBLE -> Math.max(40, Math.min(59, candidate));
            case EXCLUDED -> Math.min(39, candidate);
        };
    }

    private static int defaultScore(ExternalJobRecommendationType type) {
        if (ExternalJobRecommendationType.HIGHLY_RECOMMENDED.equals(type)) {
            return 95;
        }
        if (ExternalJobRecommendationType.RECOMMENDED.equals(type)) {
            return 70;
        }
        if (ExternalJobRecommendationType.POSSIBLE.equals(type)) {
            return 45;
        }
        return 0;
    }

    private String toJson(Map<String, String> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("INVALID_ROW_JSON");
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String failureReason(RuntimeException e) {
        if (e.getMessage() != null) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static final class ImportAccumulator {
        private final String importType;
        private final List<ExternalJobImportFailedRow> failedRows = new ArrayList<>();
        private int insertedCount;
        private int updatedCount;
        private int skippedCount;

        private ImportAccumulator(String importType) {
            this.importType = importType;
        }

        private void recordWrite(int affectedRows, boolean existed) {
            if (affectedRows <= 0) {
                skippedCount++;
                return;
            }
            if (existed || affectedRows > 1) {
                updatedCount++;
            } else {
                insertedCount++;
            }
        }

        private void fail(int rowNumber, String source, String sourceJobId, String reason) {
            skippedCount++;
            failedRows.add(ExternalJobImportFailedRow.builder()
                    .rowNumber(rowNumber)
                    .source(source)
                    .sourceJobId(sourceJobId)
                    .reason(reason)
                    .build());
        }

        private ExternalJobImportResponse toResponse() {
            return ExternalJobImportResponse.builder()
                    .importType(importType)
                    .insertedCount(insertedCount)
                    .updatedCount(updatedCount)
                    .skippedCount(skippedCount)
                    .failedCount(failedRows.size())
                    .failedRows(failedRows)
                    .build();
        }
    }
}
