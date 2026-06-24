package com.ssafy.lancit.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalJobSource {
    SEOUL("서울시 일자리플러스센터"),
    GYEONGGI("경기도 잡아바");

    private final String label;
}
