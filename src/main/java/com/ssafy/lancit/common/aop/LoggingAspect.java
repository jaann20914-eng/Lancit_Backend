package com.ssafy.lancit.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component


//모든 Service 메서드 실행 시 자동으로 시작/종료/소요시간/예외 로깅 (코드 수정 없이 AOP 로 처리)
public class LoggingAspect {
 
    /** com.lancit.domain 하위 모든 Service 메서드 진입/종료/소요시간 로깅 */
    @Around("execution(* com.ssafy.lancit.domain..service.*.*(..))")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString(); // 실행 중인 메서드 이름 가져오기
        long start = System.currentTimeMillis();
        log.debug("[SERVICE] START {}", method);
        try {
            Object result = pjp.proceed();
            log.debug("[SERVICE] END   {} ({}ms)", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.warn("[SERVICE] ERROR {} - {}", method, e.getMessage());
            throw e;
        }
    }
}