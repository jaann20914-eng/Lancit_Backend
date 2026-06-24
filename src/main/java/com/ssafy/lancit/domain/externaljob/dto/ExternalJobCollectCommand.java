package com.ssafy.lancit.domain.externaljob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobCollectCommand {
    private Integer page;
    private Integer size;
    private Integer startIndex;
    private Integer endIndex;
    private Integer maxPages;

    public static ExternalJobCollectCommand from(ExternalJobCollectRequest request) {
        if (request == null) {
            return ExternalJobCollectCommand.builder().build();
        }
        return ExternalJobCollectCommand.builder()
                .page(request.getPage())
                .size(request.getSize())
                .startIndex(request.getStartIndex())
                .endIndex(request.getEndIndex())
                .maxPages(request.getMaxPages())
                .build();
    }
}
