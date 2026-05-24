package com.ssafy.lancit.common.aop;



import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.common.annotation.ContractGuard;
import com.ssafy.lancit.common.util.SecurityUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @ContractGuard 어노테이션이 붙은 메서드 실행 전 진행 중 계약 여부 확인
 * UserService.delete(), CompanyService.delete() 에서 사용
 *
 */
@Slf4j
@Aspect
@Component
public class ContractGuardAspect {

    @Before("@annotation(contractGuard)")
    public void checkContract(ContractGuard contractGuard) {
        String email = SecurityUtil.getCurrentEmail();

        log.debug("[ContractGuard] 계약 체크 - email: {}", email);
    }
}