package com.ssafy.lancit.domain.contract.validator;

import org.springframework.stereotype.Component;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.global.enums.ContractStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractValidator {

    private final ContractMapper contractMapper;

    // 계약 조회
    public ContractDTO getContractOrThrow(Integer contractId) {
        ContractDTO contract =
                contractMapper.findById(contractId);
        if (contract == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return contract;
    }

    
    
    // 회사 검증
    public void validateCompany(ContractDTO contract) {
        String currentEmail =SecurityUtil.getCurrentEmail();
        if (!currentEmail.equals(contract.getCompanyEmail())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    
    
    // 프리랜서 검증
    public void validateFreelancer(ContractDTO contract) {
        String currentEmail = SecurityUtil.getCurrentEmail();

        if (!currentEmail.equals(contract.getFreelancerEmail())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
    
    

    // 상태 검증
    public void validateStatus(ContractDTO contract,
            				   ContractStatus status) {
        if (contract.getStatus() != status) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
    // 여러 상태 허용
    public void validateStatus(ContractDTO contract,
            				   ContractStatus... statuses) {
        for (ContractStatus status : statuses) {
            if (contract.getStatus() == status) {
                return;
            }
        }
        throw new CustomException(ErrorCode.INVALID_INPUT);
    }
    
    // 계약 파기 가능한지 검증
    public void validateCancelable(
            ContractDTO contract
    ) {

        validateStatus(
                contract,
                ContractStatus.PROPOSAL,
                ContractStatus.WAITING,
                ContractStatus.NEGOTIATING_A,
                ContractStatus.NEGOTIATING_B,
                ContractStatus.NEGOTIATING_C,
                ContractStatus.IN_PROGRESS
        );
    }
    

    // 상태별 허용 메서드
    // WAITING
    public void validateWaiting(ContractDTO contract) {
        validateStatus(contract,ContractStatus.WAITING);
    }
    // NEGOTIATING_A
    public void validateNegotiatingA(ContractDTO contract) {
        validateStatus(contract,ContractStatus.NEGOTIATING_A);
    }
    // NEGOTIATING_B
    public void validateNegotiatingB( ContractDTO contract) {
        validateStatus(contract,ContractStatus.NEGOTIATING_B);
    }
    // NEGOTIATING_C
    public void validateNegotiatingC( ContractDTO contract) {
        validateStatus(contract,ContractStatus.NEGOTIATING_C);
    }
    // IN_PROGRESS
    public void validateInProgress(ContractDTO contract) {
        validateStatus(contract, ContractStatus.IN_PROGRESS);
    }
    // COMPLETED_PENDING
    public void validateCompletedPending(ContractDTO contract) {
        validateStatus(contract,ContractStatus.COMPLETED_PENDING);
    }
    
    
    
    //협의중 전용
    public void validateNegotiating(ContractDTO contract) {
        validateStatus(
                contract,
                ContractStatus.WAITING,
                ContractStatus.NEGOTIATING_A,
                ContractStatus.NEGOTIATING_B,
                ContractStatus.NEGOTIATING_C
        );
    }
    //진행중 전용
    public void validateProgressState( ContractDTO contract) {
        validateStatus(
                contract,
                ContractStatus.IN_PROGRESS,
                ContractStatus.COMPLETED_PENDING
        );
    }
    
    
}