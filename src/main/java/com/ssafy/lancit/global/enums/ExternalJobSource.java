package com.ssafy.lancit.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalJobSource {
    SEOUL(
            "서울시 일자리플러스센터",
            "https://job.seoul.go.kr/hmpg/main/main.do?sso=ok",
            "사이트에서 확인"
    );

    private final String label;
    private final String siteUrl;
    private final String buttonLabel;
}
