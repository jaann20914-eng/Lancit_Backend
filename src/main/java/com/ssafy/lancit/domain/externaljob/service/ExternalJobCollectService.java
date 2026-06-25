package com.ssafy.lancit.domain.externaljob.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.lancit.domain.externaljob.classifier.ExternalJobClassifier;
import com.ssafy.lancit.domain.externaljob.classifier.ExternalJobClassificationPolicy;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResult;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectionLogCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.domain.externaljob.normalizer.SeoulExternalJobNormalizer;
import com.ssafy.lancit.domain.externaljob.provider.ExternalJobProvider;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobCollectionStatus;
import com.ssafy.lancit.global.enums.ExternalJobCollectionType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ExternalJobCollectService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int DEFAULT_MAX_PAGES = 1;
    private static final int MAX_PAGE_RETRY_COUNT = 3;
    private static final int NO_DEADLINE_VISIBLE_DAYS = 30;
    private static final int LOCK_TTL_HOURS = 3;
    private static final String VISIBLE_REASON = "VISIBLE";

    private final List<ExternalJobProvider> providers;
    private final SeoulExternalJobNormalizer seoulExternalJobNormalizer;
    private final ExternalJobClassifier externalJobClassifier;
    private final ExternalJobMapper externalJobMapper;
    private final Clock clock;

    public ExternalJobCollectService(List<ExternalJobProvider> providers,
                                     SeoulExternalJobNormalizer seoulExternalJobNormalizer,
                                     ExternalJobClassifier externalJobClassifier,
                                     ExternalJobMapper externalJobMapper,
                                     Clock clock) {
        this.providers = providers == null ? List.of() : providers;
        this.seoulExternalJobNormalizer = seoulExternalJobNormalizer;
        this.externalJobClassifier = externalJobClassifier;
        this.externalJobMapper = externalJobMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public ExternalJobCollectResponse collectSeoulJobs(ExternalJobCollectCommand command) {
        return collectSeoulJobs(command, ExternalJobCollectionType.MANUAL);
    }

    public ExternalJobCollectResponse reclassifySeoulJobs() {
        LocalDateTime reclassifiedAt = now();
        List<ExternalJobDTO> jobs = externalJobMapper.findExternalJobsForReclassification(ExternalJobSource.SEOUL);
        CollectionAccumulator accumulator = new CollectionAccumulator();
        accumulator.fetchedCount = jobs.size();
        accumulator.succeededPages = jobs.isEmpty() ? 0 : 1;

        for (ExternalJobDTO job : jobs) {
            try {
                ExternalJobUpsertCommand classified = applyClassification(toUpsertCommand(job, reclassifiedAt),
                        reclassifiedAt);
                if (externalJobMapper.updateExternalJobClassification(
                        job.getId(),
                        classified.getFreelanceType(),
                        classified.getRecommendationType(),
                        classified.getRecommendationScore(),
                        classified.getVisible(),
                        classified.getVisibilityReason(),
                        reclassifiedAt) > 0) {
                    accumulator.upsertedCount++;
                }
            } catch (RuntimeException e) {
                accumulator.failedCount++;
                log.warn("Failed to reclassify Seoul external job. externalJobId={}",
                        job == null ? null : job.getId(), e);
            }
        }

        ExternalJobCollectionStatus status = resolveReclassificationStatus(accumulator);
        return ExternalJobCollectResponse.builder()
                .source(ExternalJobSource.SEOUL)
                .collectionType(ExternalJobCollectionType.MANUAL)
                .status(status)
                .fetchedCount(accumulator.fetchedCount)
                .upsertedCount(accumulator.upsertedCount)
                .skippedCount(accumulator.skippedCount)
                .failedCount(accumulator.failedCount)
                .succeededPages(accumulator.succeededPages)
                .failedPages(accumulator.failedCount > 0 ? 1 : 0)
                .message("서울시 외부 공고 재분류 완료 / status=%s, fetched=%d, updated=%d, failed=%d"
                        .formatted(status,
                                accumulator.fetchedCount,
                                accumulator.upsertedCount,
                                accumulator.failedCount))
                .build();
    }

    public ExternalJobCollectResponse collectSeoulJobs(ExternalJobCollectCommand command,
                                                       ExternalJobCollectionType collectionType) {
        ExternalJobCollectionType safeCollectionType = collectionType == null
                ? ExternalJobCollectionType.MANUAL
                : collectionType;
        CollectionRange range = resolveCollectionRange(command);
        LocalDateTime startedAt = now();
        String lockOwner = "external-job-" + UUID.randomUUID();

        if (!acquireLock(ExternalJobSource.SEOUL, lockOwner, startedAt)) {
            LocalDateTime endedAt = now();
            ExternalJobCollectResponse response = ExternalJobCollectResponse.builder()
                    .source(ExternalJobSource.SEOUL)
                    .collectionType(safeCollectionType)
                    .status(ExternalJobCollectionStatus.SKIPPED_LOCKED)
                    .message("서울시 외부 공고 수집이 이미 실행 중입니다.")
                    .build();
            insertCollectionLog(response, range, startedAt, endedAt);
            return response;
        }

        ExternalJobCollectResponse response;
        try {
            response = collectSeoulJobsLocked(range, safeCollectionType);
        } catch (RuntimeException e) {
            log.warn("Failed to collect Seoul external jobs.", e);
            response = ExternalJobCollectResponse.builder()
                    .source(ExternalJobSource.SEOUL)
                    .collectionType(safeCollectionType)
                    .status(ExternalJobCollectionStatus.FAILED)
                    .failedCount(1)
                    .failedPages(1)
                    .firstFailedPage(DEFAULT_PAGE)
                    .message("서울시 외부 공고 수집 실패: " + e.getClass().getSimpleName())
                    .build();
        } finally {
            releaseLock(ExternalJobSource.SEOUL, lockOwner);
        }

        insertCollectionLog(response, range, startedAt, now());
        return response;
    }

    private ExternalJobCollectResponse collectSeoulJobsLocked(CollectionRange range,
                                                              ExternalJobCollectionType collectionType) {
        ExternalJobProvider provider = findProvider(ExternalJobSource.SEOUL);
        CollectionAccumulator accumulator = new CollectionAccumulator();
        LocalDateTime collectionNow = now();

        for (int pageIndex = 0; pageIndex < range.maxPages(); pageIndex++) {
            int pageNumber = pageIndex + 1;
            ExternalJobCollectCommand pageCommand = pageCommand(range, pageIndex);
            PageCollectResult pageResult = collectPageWithRetry(provider, pageCommand, pageNumber);
            if (!pageResult.success()) {
                accumulator.recordPageFailure(pageNumber, pageResult.message());
                if (pageIndex == 0) {
                    accumulator.firstPageFailed = true;
                }
                break;
            }

            ExternalJobCollectResult providerResult = pageResult.result();
            accumulator.succeededPages++;
            accumulator.fetchedCount += providerResult.getFetchedCount();
            accumulator.skippedCount += providerResult.getSkippedCount();
            accumulator.lastProviderMessage = providerResult.getMessage();

            if (providerResult.safeRawRows().isEmpty()) {
                break;
            }

            processRows(providerResult.safeRawRows(), accumulator, collectionNow);
        }

        if (!accumulator.firstPageFailed) {
            externalJobMapper.refreshVisibilityByPolicy(
                    ExternalJobSource.SEOUL,
                    collectionNow,
                    collectionNow.minusDays(NO_DEADLINE_VISIBLE_DAYS));
        }

        ExternalJobCollectionStatus status = resolveStatus(accumulator);
        return ExternalJobCollectResponse.builder()
                .source(ExternalJobSource.SEOUL)
                .collectionType(collectionType)
                .status(status)
                .fetchedCount(accumulator.fetchedCount)
                .upsertedCount(accumulator.upsertedCount)
                .skippedCount(accumulator.skippedCount)
                .failedCount(accumulator.failedCount)
                .succeededPages(accumulator.succeededPages)
                .failedPages(accumulator.failedPages)
                .firstFailedPage(accumulator.firstFailedPage)
                .message(resolveMessage(accumulator, status))
                .build();
    }

    private void processRows(List<JsonNode> rows, CollectionAccumulator accumulator, LocalDateTime collectionNow) {
        for (JsonNode row : rows) {
            try {
                ExternalJobUpsertCommand normalized = seoulExternalJobNormalizer.normalize(row).orElse(null);
                if (normalized == null) {
                    accumulator.skippedCount++;
                    continue;
                }

                ExternalJobUpsertCommand classified = applyClassification(normalized, collectionNow);
                if (externalJobMapper.upsertExternalJob(classified) > 0) {
                    accumulator.upsertedCount++;
                }
            } catch (RuntimeException e) {
                accumulator.failedCount++;
                log.warn("Failed to process Seoul external job row.", e);
            }
        }
    }

    private PageCollectResult collectPageWithRetry(ExternalJobProvider provider,
                                                   ExternalJobCollectCommand pageCommand,
                                                   int pageNumber) {
        ExternalJobCollectResult lastResult = null;
        RuntimeException lastException = null;

        for (int retryCount = 0; retryCount <= MAX_PAGE_RETRY_COUNT; retryCount++) {
            try {
                ExternalJobCollectResult result = provider.collect(pageCommand);
                if (result.getFailedCount() <= 0) {
                    return PageCollectResult.success(result);
                }
                lastResult = result;
            } catch (RuntimeException e) {
                lastException = e;
            }

            if (retryCount < MAX_PAGE_RETRY_COUNT) {
                log.warn("Retrying Seoul external job page collection. page={}, retry={}/{}",
                        pageNumber, retryCount + 1, MAX_PAGE_RETRY_COUNT);
            }
        }

        String message = lastResult != null && StringUtils.hasText(lastResult.getMessage())
                ? lastResult.getMessage()
                : lastException == null ? "페이지 수집 실패" : lastException.getClass().getSimpleName();
        return PageCollectResult.failure(message);
    }

    private ExternalJobUpsertCommand applyClassification(ExternalJobUpsertCommand command,
                                                         LocalDateTime collectionNow) {
        ExternalJobClassification classification =
                ExternalJobClassificationPolicy.normalize(externalJobClassifier.classify(toClassificationInput(command)));
        ExternalFreelanceType freelanceType = classification.getFreelanceType();
        ExternalJobRecommendationType recommendationType = classification.getRecommendationType();
        VisibilityDecision visibility = resolveVisibility(command, freelanceType, collectionNow);

        return command.toBuilder()
                .freelanceType(freelanceType)
                .recommendationType(recommendationType)
                .recommendationScore(classification.getRecommendationScore())
                .visible(visibility.visible())
                .visibilityReason(visibility.reason())
                .build();
    }

    private VisibilityDecision resolveVisibility(ExternalJobUpsertCommand command,
                                                 ExternalFreelanceType freelanceType,
                                                 LocalDateTime collectionNow) {
        if (ExternalFreelanceType.NOT_FREELANCE.equals(freelanceType)) {
            return new VisibilityDecision(false, "NOT_FREELANCE");
        }
        if (command.getDeadlineAt() != null && command.getDeadlineAt().isBefore(collectionNow)) {
            return new VisibilityDecision(false, "EXPIRED");
        }
        LocalDateTime staleBasis = command.getPostedAt() == null ? command.getCollectedAt() : command.getPostedAt();
        if (command.getDeadlineAt() == null
                && staleBasis != null
                && staleBasis.isBefore(collectionNow.minusDays(NO_DEADLINE_VISIBLE_DAYS))) {
            return new VisibilityDecision(false, "NO_DEADLINE_STALE");
        }
        return new VisibilityDecision(true, VISIBLE_REASON);
    }

    private ExternalJobClassificationInput toClassificationInput(ExternalJobUpsertCommand command) {
        return ExternalJobClassificationInput.builder()
                .title(command.getTitle())
                .companyName(command.getCompanyName())
                .description(command.getDescription())
                .jobCategoryRaw(command.getJobCategoryRaw())
                .employmentTypeRaw(command.getEmploymentTypeRaw())
                .location(command.getLocation())
                .salaryRaw(command.getSalaryRaw())
                .build();
    }

    private ExternalJobUpsertCommand toUpsertCommand(ExternalJobDTO job, LocalDateTime updatedAt) {
        return ExternalJobUpsertCommand.builder()
                .source(job.getSource())
                .sourceJobId(job.getSourceJobId())
                .sourceUrl(job.getSourceUrl())
                .title(job.getTitle())
                .companyName(job.getCompanyName())
                .location(job.getLocation())
                .jobCategoryRaw(job.getJobCategoryRaw())
                .employmentTypeRaw(job.getEmploymentTypeRaw())
                .salaryRaw(job.getSalaryRaw())
                .postedAt(job.getPostedAt())
                .deadlineAt(job.getDeadlineAt())
                .description(job.getDescription())
                .originalPayloadJson(job.getOriginalPayloadJson())
                .payloadHash(job.getPayloadHash())
                .freelanceType(job.getFreelanceType())
                .recommendationType(job.getRecommendationType())
                .recommendationScore(job.getRecommendationScore())
                .visible(job.getVisible())
                .visibilityReason(job.getVisibilityReason())
                .collectedAt(job.getCollectedAt())
                .updatedAt(updatedAt)
                .build();
    }

    private ExternalJobProvider findProvider(ExternalJobSource source) {
        return providers.stream()
                .filter(provider -> source.equals(provider.getSource()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("External job provider not found: " + source));
    }

    private CollectionRange resolveCollectionRange(ExternalJobCollectCommand command) {
        int maxPages = positive(command == null ? null : command.getMaxPages(), DEFAULT_MAX_PAGES);
        int size = positive(command == null ? null : command.getSize(), DEFAULT_PAGE_SIZE);
        Integer startIndex = command == null ? null : command.getStartIndex();
        Integer endIndex = command == null ? null : command.getEndIndex();
        if (startIndex != null && startIndex > 0 && endIndex != null && endIndex >= startIndex) {
            return new CollectionRange(startIndex, endIndex - startIndex + 1, maxPages);
        }

        int page = positive(command == null ? null : command.getPage(), DEFAULT_PAGE);
        return new CollectionRange(((page - 1) * size) + 1, size, maxPages);
    }

    private ExternalJobCollectCommand pageCommand(CollectionRange range, int pageIndex) {
        int startIndex = range.startIndex() + pageIndex * range.pageSize();
        int endIndex = startIndex + range.pageSize() - 1;
        return ExternalJobCollectCommand.builder()
                .startIndex(startIndex)
                .endIndex(endIndex)
                .size(range.pageSize())
                .maxPages(1)
                .build();
    }

    private ExternalJobCollectionStatus resolveStatus(CollectionAccumulator accumulator) {
        if (accumulator.firstPageFailed) {
            return ExternalJobCollectionStatus.FAILED;
        }
        if (accumulator.failedPages > 0 || accumulator.failedCount > 0) {
            return ExternalJobCollectionStatus.PARTIAL_SUCCESS;
        }
        return ExternalJobCollectionStatus.SUCCESS;
    }

    private ExternalJobCollectionStatus resolveReclassificationStatus(CollectionAccumulator accumulator) {
        if (accumulator.failedCount > 0 && accumulator.upsertedCount == 0) {
            return ExternalJobCollectionStatus.FAILED;
        }
        if (accumulator.failedCount > 0) {
            return ExternalJobCollectionStatus.PARTIAL_SUCCESS;
        }
        return ExternalJobCollectionStatus.SUCCESS;
    }

    private String resolveMessage(CollectionAccumulator accumulator, ExternalJobCollectionStatus status) {
        String providerMessage = StringUtils.hasText(accumulator.lastProviderMessage)
                ? accumulator.lastProviderMessage
                : "서울시 공고 수집 완료";
        return "%s / status=%s, fetched=%d, upserted=%d, skipped=%d, failed=%d, pages=%d/%d"
                .formatted(providerMessage,
                        status,
                        accumulator.fetchedCount,
                        accumulator.upsertedCount,
                        accumulator.skippedCount,
                        accumulator.failedCount,
                        accumulator.succeededPages,
                        accumulator.succeededPages + accumulator.failedPages);
    }

    private boolean acquireLock(ExternalJobSource source, String lockOwner, LocalDateTime lockedAt) {
        return externalJobMapper.tryAcquireCollectionLock(
                source,
                lockOwner,
                lockedAt,
                lockedAt.plusHours(LOCK_TTL_HOURS),
                lockedAt) > 0;
    }

    private void releaseLock(ExternalJobSource source, String lockOwner) {
        try {
            externalJobMapper.releaseCollectionLock(source, lockOwner, now());
        } catch (RuntimeException e) {
            log.warn("Failed to release external job collection lock. source={}", source, e);
        }
    }

    private void insertCollectionLog(ExternalJobCollectResponse response,
                                     CollectionRange range,
                                     LocalDateTime startedAt,
                                     LocalDateTime endedAt) {
        externalJobMapper.insertCollectionLog(ExternalJobCollectionLogCommand.builder()
                .source(response.getSource())
                .collectionType(response.getCollectionType())
                .status(response.getStatus())
                .requestedPageSize(range.pageSize())
                .requestedMaxPages(range.maxPages())
                .fetchedCount(response.getFetchedCount())
                .upsertedCount(response.getUpsertedCount())
                .skippedCount(response.getSkippedCount())
                .failedCount(response.getFailedCount())
                .succeededPages(response.getSucceededPages())
                .failedPages(response.getFailedPages())
                .firstFailedPage(response.getFirstFailedPage())
                .message(response.getMessage())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .createdAt(endedAt)
                .build());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private int positive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private record CollectionRange(int startIndex, int pageSize, int maxPages) {
    }

    private record PageCollectResult(boolean success, ExternalJobCollectResult result, String message) {
        private static PageCollectResult success(ExternalJobCollectResult result) {
            return new PageCollectResult(true, result, null);
        }

        private static PageCollectResult failure(String message) {
            return new PageCollectResult(false, null, message);
        }
    }

    private record VisibilityDecision(boolean visible, String reason) {
    }

    private static final class CollectionAccumulator {
        private int fetchedCount;
        private int upsertedCount;
        private int skippedCount;
        private int failedCount;
        private int succeededPages;
        private int failedPages;
        private Integer firstFailedPage;
        private boolean firstPageFailed;
        private String lastProviderMessage;

        private void recordPageFailure(int pageNumber, String message) {
            failedPages++;
            failedCount++;
            if (firstFailedPage == null) {
                firstFailedPage = pageNumber;
            }
            lastProviderMessage = message;
        }
    }
}
