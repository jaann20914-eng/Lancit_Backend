package com.ssafy.lancit.domain.externaljob.dto;

import com.ssafy.lancit.global.enums.ExternalJobCollectionStatus;
import com.ssafy.lancit.global.enums.ExternalJobCollectionType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobCollectionLogCommand {
    private ExternalJobSource source;
    private ExternalJobCollectionType collectionType;
    private ExternalJobCollectionStatus status;
    private Integer requestedPageSize;
    private Integer requestedMaxPages;
    private Integer fetchedCount;
    private Integer upsertedCount;
    private Integer skippedCount;
    private Integer failedCount;
    private Integer succeededPages;
    private Integer failedPages;
    private Integer firstFailedPage;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
}
