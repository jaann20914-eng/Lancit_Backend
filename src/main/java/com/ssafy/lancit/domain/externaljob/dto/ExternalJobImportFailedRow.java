package com.ssafy.lancit.domain.externaljob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobImportFailedRow {
    private int rowNumber;
    private String source;
    private String sourceJobId;
    private String reason;
}
