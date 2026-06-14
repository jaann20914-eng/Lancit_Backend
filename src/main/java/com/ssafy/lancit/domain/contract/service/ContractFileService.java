package com.ssafy.lancit.domain.contract.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.annotation.ParticipantCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.dto.ContractFileDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractFileMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.contract.validator.ContractValidator;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.global.enums.ContractFileType;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.NotificationType;

import lombok.RequiredArgsConstructor;

// 컨펌 파일 
// 컨펌 파일 업로드 : 조회 : 삭제
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractFileService {

	private final ContractMapper contractMapper;
    private final ContractFileMapper contractFileMapper;
    private final FileService fileService;
    private final ContractValidator contractValidator; //유효성 메서드 모음
    private final NotificationService notificationService;
    
	// 계약서 pdf파일 업로드는
    // ContractService의 approveContract메서드에서 바로 ContractFileMapper로 보내서 인서트 함
    
    
    
    //컨펌파일 업로드
    @Transactional
    @ParticipantCheck  //지금 토큰가지고 있는 사람이 이 계약의 소유권자인지 확인 어노테이션
    public void uploadConfirmFile(Integer contractId,
            					  Integer fileId) {
    	//계약아이디 유효성 검사
        ContractDTO contract = contractValidator.getContractOrThrow(contractId); 
        //프리랜서인지 유효성 검사
        contractValidator.validateFreelancer(contract);
        
        
        // 계약진행중 상태 아니라면 컨펌파일 업로드 불가
        if (contract.getStatus() != ContractStatus.IN_PROGRESS) {throw new CustomException(ErrorCode.INVALID_INPUT);}

        //파일 아이디 가지고 contractFileMapper에 인서트하기
        fileService.findById(fileId);
        ContractFileDTO dto = ContractFileDTO.builder()
                .contractId(contractId)
                .fileId(fileId)
                .type(ContractFileType.CONFIRM)
                .uploaderEmail(SecurityUtil.getCurrentEmail())
                .build();

        contractFileMapper.insert(dto);
        
        //알림 발송
        notificationService.createNotification(
                contract.getCompanyEmail(),
                NotificationType.CONFIRM_FILE,
                contractId
        );
    }
	
    
    
    // 계약아이디로 컨펌파일 전부 가져오기
    public List<ContractFileDTO> getConfirmFiles(
            Integer contractId) {
    	//계약아이디 유효성 검사
    	contractValidator.getContractOrThrow(contractId);
        //계약아이디로 컨펌 파일 찾기
        return contractFileMapper.findConfirmFilesByContractId(contractId);
    }
    
    // 계약서 pdf파일은 
    // ContractService의 deleteContractPdf 메서드에서 바로 ContractFileMapper로 보내서 삭제 함
    
    
    @Transactional
    @ParticipantCheck
    public void deleteConfirmFile(
            Integer contractId,
            Integer contractFileId) {
    	
    	//계약아이디 유효성 검사
        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        //프리랜서인지 유효성 검사
        contractValidator.validateFreelancer(contract);
        
        // 계약진행중 상태 아니라면 컨펌파일 삭제 불가
        if (contract.getStatus() != ContractStatus.IN_PROGRESS) {throw new CustomException(ErrorCode.INVALID_INPUT);}

        // 계약 파일가져와서
        ContractFileDTO file =contractFileMapper.findById(contractFileId);
        if (file == null) {throw new CustomException(ErrorCode.NOT_FOUND);}

        //계약파일테이블부터 삭제
        contractFileMapper.delete(contractFileId);
        //파일테이블 삭제
        fileService.delete(file.getFileId());
    }
    
}
