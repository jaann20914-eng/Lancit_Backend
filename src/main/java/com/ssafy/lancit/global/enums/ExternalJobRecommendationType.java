package com.ssafy.lancit.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalJobRecommendationType {
    HIGHLY_RECOMMENDED("매우 추천"),
    RECOMMENDED("추천"),
    POSSIBLE("검토 가능"),
    EXCLUDED("제외");

    private final String label;
}
