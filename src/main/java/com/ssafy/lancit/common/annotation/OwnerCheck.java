package com.ssafy.lancit.common.annotation;

import java.lang.annotation.*;

/**
 * 리소스 소유자 검증 어노테이션: 내게 아닌 db 데이터를 파일아이디 , 계약 아이디 등으로 접근하려고 하면 막을 수 있는 어노테이션
 * OwnerCheckAspect 에서 첫 번째 파라미터(resourceId)를 꺼내 DB 조회 후 현재 로그인 이메일과 비교
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OwnerCheck {
    String resourceType(); // "TASK" | "PORTFOLIO" | "CONTRACT" | "MESSAGE" | "CATEGORY"
}