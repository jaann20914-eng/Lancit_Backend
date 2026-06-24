package com.ssafy.lancit.domain.externaljob.normalizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulExternalJobNormalizer {

    private static final int SOURCE_JOB_ID_MAX_LENGTH = 100;
    private static final int SOURCE_URL_MAX_LENGTH = 1000;
    private static final int TITLE_MAX_LENGTH = 300;
    private static final int COMPANY_MAX_LENGTH = 200;
    private static final int LOCATION_MAX_LENGTH = 300;
    private static final int RAW_200_MAX_LENGTH = 200;
    private static final int SALARY_MAX_LENGTH = 300;
    private static final int DESCRIPTION_MAX_LENGTH = 60000;
    private static final Pattern FLEXIBLE_DATE_PATTERN =
            Pattern.compile("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})");
    private static final DateTimeFormatter BASIC_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private static final List<String> SOURCE_JOB_ID_FIELDS = List.of(
            "구인신청번호", "구인등록번호", "JO_REQST_NO", "JO_REGIST_NO", "JO_NO",
            "J_ID", "JOB_ID", "sourceJobId", "jobId"
    );
    private static final List<String> TITLE_FIELDS = List.of(
            "공고제목", "구인제목", "채용제목", "JO_SJ", "JOB_TITLE", "TITLE", "title"
    );
    private static final List<String> COMPANY_FIELDS = List.of(
            "기업명칭", "회사명", "CMPNY_NM", "COMPANY_NAME", "companyName"
    );
    private static final List<String> DESCRIPTION_FIELDS = List.of(
            "사업요약내용", "직무내용", "공고내용", "BSNS_SUMRY_CN", "DTY_CN", "JOB_CN",
            "JO_CN", "DESCRIPTION", "description"
    );
    private static final List<String> CATEGORY_FIELDS = List.of(
            "모집직종명", "모집직종코드", "RCRIT_JSSFC_CMMN_CODE_SE_NM",
            "RCRIT_JSSFC_CMMN_CODE_SE", "JOB_CATEGORY", "jobCategoryRaw"
    );
    private static final List<String> EMPLOYMENT_FIELDS = List.of(
            "고용형태", "고용형태명", "EMPLYM_STLE_CMMN_CODE_SE_NM",
            "EMPLYM_STLE_CMMN_CODE_SE", "EMPLOYMENT_TYPE", "employmentTypeRaw"
    );
    private static final List<String> LOCATION_FIELDS = List.of(
            "근무예정지 주소", "근무예정지주소", "WORK_PARAR_BASS_ADRES_CN",
            "WORK_PARAR_ROAD_NM_BASS_ADRES_CN", "BASS_ADRES_CN", "ADDRESS", "location"
    );
    private static final List<String> SALARY_FIELDS = List.of(
            "급여", "급여조건", "임금", "HOPE_WAGE", "WAGE_CN", "SALARY", "salaryRaw"
    );
    private static final List<String> POSTED_AT_FIELDS = List.of(
            "등록일", "JO_REG_DT", "REG_DT", "CREATE_DT", "postedAt"
    );
    private static final List<String> DEADLINE_FIELDS = List.of(
            "마감일", "접수마감일", "RCEPT_CLOS_DT", "RCEPT_CLOS_NM", "DEADLINE", "deadlineAt"
    );
    private static final List<String> SOURCE_URL_FIELDS = List.of(
            "상세URL", "상세 URL", "JO_URL", "DETAIL_URL", "URL", "sourceUrl"
    );

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public Optional<ExternalJobUpsertCommand> normalize(JsonNode row) {
        if (row == null || row.isNull() || row.isMissingNode()) {
            return Optional.empty();
        }

        String sourceJobId = truncate(firstText(row, SOURCE_JOB_ID_FIELDS), SOURCE_JOB_ID_MAX_LENGTH);
        if (!StringUtils.hasText(sourceJobId)) {
            return Optional.empty();
        }

        String companyName = truncate(firstText(row, COMPANY_FIELDS), COMPANY_MAX_LENGTH);
        String jobCategoryRaw = truncate(firstText(row, CATEGORY_FIELDS), RAW_200_MAX_LENGTH);
        String description = truncate(firstText(row, DESCRIPTION_FIELDS), DESCRIPTION_MAX_LENGTH);
        String title = truncate(resolveTitle(firstText(row, TITLE_FIELDS), companyName, jobCategoryRaw, description),
                TITLE_MAX_LENGTH);
        if (!StringUtils.hasText(title)) {
            return Optional.empty();
        }

        String originalPayloadJson = toJson(row);
        LocalDateTime now = LocalDateTime.now(clock);

        return Optional.of(ExternalJobUpsertCommand.builder()
                .source(ExternalJobSource.SEOUL)
                .sourceJobId(sourceJobId)
                .sourceUrl(truncate(firstText(row, SOURCE_URL_FIELDS), SOURCE_URL_MAX_LENGTH))
                .title(title)
                .companyName(companyName)
                .location(truncate(firstText(row, LOCATION_FIELDS), LOCATION_MAX_LENGTH))
                .jobCategoryRaw(jobCategoryRaw)
                .employmentTypeRaw(truncate(firstText(row, EMPLOYMENT_FIELDS), RAW_200_MAX_LENGTH))
                .salaryRaw(truncate(firstText(row, SALARY_FIELDS), SALARY_MAX_LENGTH))
                .postedAt(parseDateTime(firstText(row, POSTED_AT_FIELDS)))
                .deadlineAt(parseDateTime(firstText(row, DEADLINE_FIELDS)))
                .description(description)
                .originalPayloadJson(originalPayloadJson)
                .payloadHash(sha256(originalPayloadJson))
                .freelanceType(ExternalFreelanceType.UNKNOWN)
                .recommendationType(ExternalJobRecommendationType.POSSIBLE)
                .collectedAt(now)
                .updatedAt(now)
                .build());
    }

    private String resolveTitle(String rawTitle, String companyName, String jobCategoryRaw, String description) {
        if (StringUtils.hasText(rawTitle)) {
            return rawTitle;
        }
        if (StringUtils.hasText(companyName) && StringUtils.hasText(jobCategoryRaw)) {
            return companyName + " " + jobCategoryRaw;
        }
        if (StringUtils.hasText(jobCategoryRaw)) {
            return jobCategoryRaw;
        }
        if (StringUtils.hasText(description)) {
            return truncate(description.replaceAll("\\s+", " "), 80);
        }
        return null;
    }

    private String firstText(JsonNode row, List<String> fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = row.get(fieldName);
            String value = textValue(node);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.isValueNode() ? node.asText() : node.toString();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if ("-".equals(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.contains("채용시") || trimmed.contains("상시")) {
            return null;
        }

        Matcher flexibleMatcher = FLEXIBLE_DATE_PATTERN.matcher(trimmed);
        if (flexibleMatcher.find()) {
            return toDateTime(
                    parseInt(flexibleMatcher.group(1)),
                    parseInt(flexibleMatcher.group(2)),
                    parseInt(flexibleMatcher.group(3)));
        }

        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        if (digitsOnly.length() >= 8) {
            try {
                return LocalDate.parse(digitsOnly.substring(0, 8), BASIC_DATE_FORMATTER).atStartOfDay();
            } catch (Exception e) {
                log.debug("Failed to parse Seoul date. value={}", value);
            }
        }
        return null;
    }

    private LocalDateTime toDateTime(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(String value) {
        return Integer.parseInt(value);
    }

    private String toJson(JsonNode row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (Exception e) {
            return row.toString();
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate payload hash", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
