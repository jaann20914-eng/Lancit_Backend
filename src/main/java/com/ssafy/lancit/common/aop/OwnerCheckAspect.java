package com.ssafy.lancit.common.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @OwnerCheck 어노테이션이 붙은 메서드 실행 전 소유자 검증
 *
 * 동작 원칙:
 *  - 메서드 첫 번째 파라미터를 resourceId(int/Integer)로 가정
 *  - resourceType에 따라 적절한 Mapper를 호출하여 ownerEmail 조회
 *  - 현재 로그인 이메일과 불일치 시 FORBIDDEN 예외

 */
@Slf4j
@Aspect
@Component
public class OwnerCheckAspect {
 

 
    @Before("@annotation(ownerCheck)")
    public void checkOwner(JoinPoint jp, OwnerCheck ownerCheck) {
        String currentEmail = SecurityUtil.getCurrentEmail();
        Object[] args = jp.getArgs();
 
        if (args.length == 0) return;
        int resourceId = (int) args[0]; // 첫 번째 파라미터 = resourceId
 
        String ownerEmail = findOwnerEmail(ownerCheck.resourceType(), resourceId);
        if (!currentEmail.equals(ownerEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
 
    private String findOwnerEmail(String resourceType, int resourceId) {

        throw new UnsupportedOperationException("OwnerCheckAspect 미구현: " + resourceType);
    }
}