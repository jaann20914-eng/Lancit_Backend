package com.ssafy.lancit.domain.externaljob.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobCollectResult {
    private ExternalJobSource source;
    private int fetchedCount;
    private int upsertedCount;
    private int skippedCount;
    private int failedCount;
    private String message;
    private List<JsonNode> rawRows;

    public List<JsonNode> safeRawRows() {
        return rawRows == null ? List.of() : rawRows;
    }
}
