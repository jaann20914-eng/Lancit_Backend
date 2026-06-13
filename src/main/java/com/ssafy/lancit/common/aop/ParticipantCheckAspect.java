package com.ssafy.lancit.common.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.common.annotation.ParticipantCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ParticipantCheckAspect {
    private final ContractMapper contractMapper;

    @Before("@annotation(participantCheck)")
    public void checkParticipant(
            JoinPoint jp,
            ParticipantCheck participantCheck
    ) {

        String currentEmail = SecurityUtil.getCurrentEmail();
        Object[] args = jp.getArgs();

        if (args.length == 0) {throw new CustomException(ErrorCode.INVALID_INPUT);}

        Integer contractId = (Integer) args[0]; // 계약 아이디 첫번째 인자로 넣어줘야함

        boolean isParticipant =
                contractMapper.isParticipant(
                        contractId,
                        currentEmail
                );

        if (!isParticipant) {
            log.warn(
                    "[ParticipantCheck] 접근 거부 - contractId: {}, email: {}",
                    contractId,
                    currentEmail
            );

            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        log.debug(
                "[ParticipantCheck] 검증 통과 - contractId: {}, email: {}",
                contractId,
                currentEmail
        );
    }
}
