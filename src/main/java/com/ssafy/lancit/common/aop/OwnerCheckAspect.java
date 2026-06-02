package com.ssafy.lancit.common.aop;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;



//@OwnerCheck 커스텀 어노테이션이 붙은 메서드 실행 전 DB 조회해서 현재 로그인 유저가 해당 리소스 소유자인지 검증 (아니면 FORBIDDEN)

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OwnerCheckAspect {

	//계층 구조 원칙상 AOP 가 Mapper 에 직접 접근하는 건 맞지 않지만
	//소유자 확인이라는 단순 조회 목적이기 때문에 서비스 레이어 추가로 줄줄이 안만들고 매퍼로 접근해서 확인
    private final TaskMapper taskMapper;
    private final CategoryMapper categoryMapper;
    private final PortfolioMapper portfolioMapper;
    private final ContractMapper contractMapper;
    private final FileMapper fileMapper;

    
    // @OwnerCheck 달린 메서드 실행 직전에 동작
    @Before("@annotation(ownerCheck)")
    public void checkOwner(JoinPoint jp, OwnerCheck ownerCheck) {
        String currentEmail = SecurityUtil.getCurrentEmail();
        Object[] args = jp.getArgs();

        if (args.length == 0) return;
        int resourceId = (int) args[0]; // 메서드 첫 번째 파라미터 = resourceId**** 이거 서비스 메서드 짤때의 컨벤션으로 두기

        String ownerEmail = findOwnerEmail(ownerCheck.resourceType(), resourceId);
	     // resourceType 에 따라 DB 에서 소유자 이메일 조회
	     // TASK      → task 테이블에서 email 조회
	     // CATEGORY  → category 테이블에서 email 조회
	     // PORTFOLIO → portfolio 테이블에서 email 조회
	     // CONTRACT  → contract 테이블에서 freelancer_email 조회
	     // FILE      → file 테이블에서 user_email or company_email 조회

        
        
        if (ownerEmail == null) { // 리소스 자체가 없으면 NOT_FOUND
            log.warn("[OwnerCheck] 리소스 없음 - type: {}, id: {}", ownerCheck.resourceType(), resourceId);
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        if (!currentEmail.equals(ownerEmail)) { // 내 이메일 != 소유자 이메일 → FORBIDDEN
            log.warn("[OwnerCheck] 접근 거부 - email: {}, ownerEmail: {}, type: {}, id: {}",
                    currentEmail, ownerEmail, ownerCheck.resourceType(), resourceId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        
        
        log.debug("[OwnerCheck] 검증 통과 - type: {}, id: {}", ownerCheck.resourceType(), resourceId);
    }

    private String findOwnerEmail(String resourceType, int resourceId) {
        return switch (resourceType) {
            case "TASK"      -> taskMapper.findOwnerEmailById(resourceId);
            case "CATEGORY"  -> categoryMapper.findOwnerEmailById(resourceId);
            case "PORTFOLIO" -> portfolioMapper.findOwnerEmailById(resourceId);
            case "CONTRACT"  -> contractMapper.findOwnerEmailById(resourceId);
            case "FILE"      -> fileMapper.findOwnerEmailById(resourceId);
            default -> throw new CustomException(ErrorCode.INVALID_RESOURCE_TYPE);
        };
    }
}