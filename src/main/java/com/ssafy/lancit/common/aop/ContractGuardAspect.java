package com.ssafy.lancit.common.aop;



import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.common.annotation.ContractGuard;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


// @ContractGuard 커스텀 어노테이션이 붙은 메서드 실행 전 진행 중 계약 있으면 탈퇴 차단 (IN_PROGRESS / COMPLETED_PENDING)
// UserService.delete(), CompanyService.delete() 에서 사용 -> IN_PROGRESS 또는 COMPLETED_PENDING 계약이 있으면 탈퇴 불가
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ContractGuardAspect {

   private final ContractMapper contractMapper;

   @Before("@annotation(contractGuard)")
   public void checkContract(ContractGuard contractGuard) {
       String email = SecurityUtil.getCurrentEmail(); // 1. 현재 로그인한 이메일 꺼내기 
       log.debug("[ContractGuard] 계약 체크 - email: {}", email);

       boolean hasActiveContract = contractMapper.existsActiveContractByEmail(email); // 2. DB 에서 진행 중 계약 있는지 확인

       if (hasActiveContract) {
           log.warn("[ContractGuard] 진행 중 계약 존재 → 탈퇴 불가 - email: {}", email);
           throw new CustomException(ErrorCode.CONTRACT_IN_PROGRESS); // 3. 있으면 탈퇴 차단
       }
   }
}