package com.ssafy.lancit.common.annotation;


import java.lang.annotation.*;

/**
 * 탈퇴 전 진행 중 계약 체크 어노테이션 : 진행중인 계약있으면 탈퇴 불가능하도록
 * ContractGuardAspect 에서 현재 로그인 이메일로 IN_PROGRESS / COMPLETED_PENDING 계약 존재 여부 확인
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ContractGuard {
}