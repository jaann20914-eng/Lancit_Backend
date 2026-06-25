package com.ssafy.lancit.domain.externaljob.scheduler;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResponse;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobCollectService;
import com.ssafy.lancit.global.enums.ExternalJobCollectionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalJobCollectionScheduler {

    private static final int PAGE_SIZE = 100;
    private static final int DAILY_MAX_PAGES = 3;
    private static final int DEEP_SYNC_MAX_PAGES = 10;

    private final ExternalJobCollectService externalJobCollectService;

    @Scheduled(cron = "0 30 2 * * *", zone = "Asia/Seoul")
    public void collectSeoulExternalJobsDaily() {
        ExternalJobCollectResponse response = externalJobCollectService.collectSeoulJobs(
                command(DAILY_MAX_PAGES),
                ExternalJobCollectionType.DAILY);
        log.info("Seoul external job daily collection finished. status={}, fetched={}, upserted={}, failed={}",
                response.getStatus(), response.getFetchedCount(), response.getUpsertedCount(), response.getFailedCount());
    }

    @Scheduled(cron = "0 30 3 * * SUN", zone = "Asia/Seoul")
    public void collectSeoulExternalJobsWeeklyDeepSync() {
        ExternalJobCollectResponse response = externalJobCollectService.collectSeoulJobs(
                command(DEEP_SYNC_MAX_PAGES),
                ExternalJobCollectionType.DEEP_SYNC);
        log.info("Seoul external job deep sync finished. status={}, fetched={}, upserted={}, failed={}",
                response.getStatus(), response.getFetchedCount(), response.getUpsertedCount(), response.getFailedCount());
    }

    private ExternalJobCollectCommand command(int maxPages) {
        return ExternalJobCollectCommand.builder()
                .page(1)
                .size(PAGE_SIZE)
                .maxPages(maxPages)
                .build();
    }
}
