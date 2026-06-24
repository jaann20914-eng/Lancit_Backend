package com.ssafy.lancit.domain.externaljob.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResult;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SeoulExternalJobProvider implements ExternalJobProvider {

    private static final String DEFAULT_BASE_URL = "http://openapi.seoul.go.kr:8088";
    private static final String DEFAULT_SERVICE_NAME = "GetJobInfo";
    private static final String DEFAULT_TYPE = "json";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int DEFAULT_MAX_PAGES = 1;
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 5_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Autowired
    public SeoulExternalJobProvider(ObjectMapper objectMapper, Environment environment) {
        this(createTimeoutRestClient(environment), objectMapper, environment);
    }

    SeoulExternalJobProvider(RestClient restClient, ObjectMapper objectMapper, Environment environment) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    public ExternalJobSource getSource() {
        return ExternalJobSource.SEOUL;
    }

    @Override
    public ExternalJobCollectResult collect(ExternalJobCollectCommand command) {
        SeoulConfig config = getConfig();
        if (!config.enabled()) {
            return ExternalJobCollectResult.builder()
                    .source(getSource())
                    .failedCount(1)
                    .message("서울시 외부 공고 수집이 비활성화되어 있습니다.")
                    .rawRows(List.of())
                    .build();
        }
        config.validateApiKey();

        ResolvedRange range = resolveRange(command, config.defaultPageSize());
        List<JsonNode> rows = new ArrayList<>();
        int failedCount = 0;
        String lastMessage = null;

        for (int pageIndex = 0; pageIndex < range.maxPages(); pageIndex++) {
            int startIndex = range.startIndex() + pageIndex * range.pageSize();
            int endIndex = range.endIndex() + pageIndex * range.pageSize();
            try {
                PageFetchResult pageResult = fetchPage(config, startIndex, endIndex);
                rows.addAll(pageResult.rows());
                lastMessage = pageResult.message();
                if (pageResult.rows().isEmpty()) {
                    break;
                }
            } catch (RuntimeException e) {
                failedCount++;
                lastMessage = "서울시 API 호출 중 오류가 발생했습니다: " + e.getClass().getSimpleName();
                log.warn("Failed to fetch Seoul external jobs. startIndex={}, endIndex={}",
                        startIndex, endIndex, e);
                break;
            }
        }

        return ExternalJobCollectResult.builder()
                .source(getSource())
                .fetchedCount(rows.size())
                .failedCount(failedCount)
                .message(StringUtils.hasText(lastMessage) ? lastMessage : "서울시 공고 조회 완료")
                .rawRows(rows)
                .build();
    }

    private PageFetchResult fetchPage(SeoulConfig config, int startIndex, int endIndex) {
        String url = config.baseUrl() + "/" + config.apiKey() + "/" + config.type()
                + "/" + config.serviceName() + "/" + startIndex + "/" + endIndex + "/";

        ResponseEntity<String> response = restClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class);

        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            throw new IllegalStateException("서울시 API 응답이 비어 있습니다.");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("서울시 API JSON 응답을 파싱할 수 없습니다.", e);
        }
        JsonNode serviceNode = serviceNode(root, config.serviceName());
        logResult(serviceNode, root);

        String message = resultMessage(serviceNode, root);
        JsonNode rowNode = serviceNode.path("row");
        if (!rowNode.isArray()) {
            return new PageFetchResult(List.of(), message);
        }

        List<JsonNode> rows = new ArrayList<>();
        rowNode.forEach(rows::add);
        return new PageFetchResult(rows, message);
    }

    private JsonNode serviceNode(JsonNode root, String serviceName) {
        JsonNode configuredNode = root.path(serviceName);
        if (!configuredNode.isMissingNode()) {
            return configuredNode;
        }
        JsonNode defaultNode = root.path(DEFAULT_SERVICE_NAME);
        if (!defaultNode.isMissingNode()) {
            return defaultNode;
        }
        return root;
    }

    private void logResult(JsonNode serviceNode, JsonNode root) {
        String totalCount = serviceNode.path("list_total_count").asText(null);
        JsonNode resultNode = resultNode(serviceNode, root);
        String code = resultNode.path("CODE").asText(null);
        String message = resultNode.path("MESSAGE").asText(null);
        log.info("Seoul external job API result. totalCount={}, code={}, message={}",
                totalCount, code, message);
    }

    private String resultMessage(JsonNode serviceNode, JsonNode root) {
        JsonNode resultNode = resultNode(serviceNode, root);
        String code = resultNode.path("CODE").asText(null);
        String message = resultNode.path("MESSAGE").asText(null);
        if (StringUtils.hasText(code) || StringUtils.hasText(message)) {
            return "서울시 API 응답: " + value(code) + " " + value(message);
        }
        return "서울시 공고 조회 완료";
    }

    private JsonNode resultNode(JsonNode serviceNode, JsonNode root) {
        JsonNode resultNode = serviceNode.path("RESULT");
        return resultNode.isMissingNode() ? root.path("RESULT") : resultNode;
    }

    private ResolvedRange resolveRange(ExternalJobCollectCommand command, int defaultPageSize) {
        int maxPages = positive(command == null ? null : command.getMaxPages(), DEFAULT_MAX_PAGES);
        int size = positive(command == null ? null : command.getSize(), defaultPageSize);
        Integer start = command == null ? null : command.getStartIndex();
        Integer end = command == null ? null : command.getEndIndex();

        if (start != null && start > 0 && end != null && end >= start) {
            return new ResolvedRange(start, end, end - start + 1, maxPages);
        }

        int page = positive(command == null ? null : command.getPage(), DEFAULT_PAGE);
        int startIndex = ((page - 1) * size) + 1;
        int endIndex = startIndex + size - 1;
        return new ResolvedRange(startIndex, endIndex, size, maxPages);
    }

    private SeoulConfig getConfig() {
        return new SeoulConfig(
                trimTrailingSlash(getProperty("external-job.seoul.base-url", DEFAULT_BASE_URL)),
                getProperty("external-job.seoul.api-key", ""),
                getProperty("external-job.seoul.service-name", DEFAULT_SERVICE_NAME),
                getProperty("external-job.seoul.type", DEFAULT_TYPE),
                defaultPageSize(),
                getBooleanProperty("external-job.seoul.enabled", true)
        );
    }

    private int defaultPageSize() {
        String configuredPageSize = getProperty("external-job.seoul.default-page-size", null);
        if (StringUtils.hasText(configuredPageSize)) {
            try {
                int size = Integer.parseInt(configuredPageSize);
                if (size > 0) {
                    return size;
                }
            } catch (NumberFormatException ignored) {
                return DEFAULT_PAGE_SIZE;
            }
        }

        return DEFAULT_PAGE_SIZE;
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

    private boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        String value = getProperty(propertyName, null);
        return StringUtils.hasText(value) ? Boolean.parseBoolean(value) : defaultValue;
    }

    private static RestClient createTimeoutRestClient(Environment environment) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeout(environment,
                "external-job.seoul.connect-timeout-ms",
                DEFAULT_CONNECT_TIMEOUT_MILLIS)));
        requestFactory.setReadTimeout(Duration.ofMillis(timeout(environment,
                "external-job.seoul.read-timeout-ms",
                DEFAULT_READ_TIMEOUT_MILLIS)));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private static int timeout(Environment environment, String propertyName, int defaultValue) {
        if (environment == null) {
            return defaultValue;
        }
        String value = environment.getProperty(propertyName);
        try {
            return StringUtils.hasText(value) ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int positive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static boolean isConfigured(String value) {
        return StringUtils.hasText(value) && !isUnresolvedPlaceholder(value);
    }

    private static boolean isUnresolvedPlaceholder(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.startsWith("${") && trimmedValue.endsWith("}");
    }

    private record SeoulConfig(String baseUrl,
                               String apiKey,
                               String serviceName,
                               String type,
                               int defaultPageSize,
                               boolean enabled) {
        private void validateApiKey() {
            if (!isConfigured(apiKey)) {
                throw new CustomException(ErrorCode.SEOUL_API_KEY_MISSING);
            }
        }
    }

    private record ResolvedRange(int startIndex, int endIndex, int pageSize, int maxPages) {
    }

    private record PageFetchResult(List<JsonNode> rows, String message) {
    }
}
