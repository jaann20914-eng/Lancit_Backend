package com.ssafy.lancit.domain.externaljob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobImportResponse {
    private String importType;
    private int insertedCount;
    private int updatedCount;
    private int skippedCount;
    private int failedCount;
    private List<ExternalJobImportFailedRow> failedRows;
}
