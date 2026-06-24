package com.ssafy.lancit.domain.contract.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.common.annotation.ParticipantCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.util.BytesMultipartFile;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.chat.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.chat.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.contract.dto.ContractCancelRequestDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDocumentDTO;
import com.ssafy.lancit.domain.contract.dto.ContractFileDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractCancelRequestMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractDocumentMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractFileMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.contract.validator.ContractValidator;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationMapper;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.global.enums.ContractFileType;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.NotificationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractMapper contractMapper;
    private final ContractDocumentMapper contractDocumentMapper;
    private final ContractCancelRequestMapper contractCancelRequestMapper;
    private final ContractValidator contractValidator;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ContractPdfService contractPdfService;
    private final ContractFileMapper contractFileMapper;
    private final FileService fileService;
    private final ChatRoomMapper chatRoomMapper;
    private final RecruitmentMapper recruitmentMapper;
    private final ApplicationMapper applicationMapper;
  

    
  //==================================================================== 조회 관련 + 임시저장
    // 계약 목록 조회
    public PageResponse<Map<String, Object>> getContracts(
            PageRequest pageRequest,
            String status,
            String keywordType,
            String keyword) {

        String currentEmail = SecurityUtil.getCurrentEmail();

        Map<String, Object> param = new HashMap<>();
        param.put("email", currentEmail);
        param.put("status", status);
        param.put("keywordType", keywordType);
        param.put("keyword", keyword);
        param.put("offset", pageRequest.getOffset());
        param.put("size", pageRequest.getSize());

        long total = contractMapper.countContracts(param);
        List<Map<String, Object>> list = contractMapper.searchContracts(param);

        return PageResponse.of(list, total, pageRequest);
    }


    // 계약 상세 조회
    @ParticipantCheck
    public Map<String, Object> getContractDetail(Integer contractId) {

        Map<String, Object> detail = contractMapper.findContractDetail(contractId);
        if (detail == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        // 계약 문서
        ContractDocumentDTO document = contractDocumentMapper.findByContractId(contractId);
        detail.put("document", document);

        // 컨펌파일 목록
        List<Map<String, Object>> confirmFiles = contractFileMapper.findConfirmFilesByContractId(contractId);
        detail.put("confirmFiles", confirmFiles);

        // PDF 파일
        ContractFileDTO pdfFile = contractFileMapper.findPdfByContractId(contractId);
        detail.put("pdfFile", pdfFile);

        // 파기 요청 정보
        ContractCancelRequestDTO cancelRequest = contractCancelRequestMapper.findByContractId(contractId);
        detail.put("cancelRequest", cancelRequest);

        // 계약 관련 알림 읽음 처리
        String currentEmail = SecurityUtil.getCurrentEmail();
        notificationService.markContractNotificationsAsRead(currentEmail, contractId);

        return detail;
    }
    
    
     // 처음 회사가 제안하고 프리랜서가 거절했을 때
	 // 제안 거절 (PROPOSAL 상태, 프리랜서 전용)
	 // "삭제요청후 삭제 확정한것과 동일" → cancel-request 절차 없이 즉시 완전 삭제
	 @Transactional
	 @ParticipantCheck
	 public void rejectContract(Integer contractId) {
	
	     ContractDTO contract = contractValidator.getContractOrThrow(contractId);
	     contractValidator.validateFreelancer(contract);
	     if (contract.getStatus() != ContractStatus.PROPOSAL) {
	    	    throw new CustomException(ErrorCode.INVALID_INPUT);
	    	}
	
	     contractMapper.deleteContract(contractId);
	 }
    
    
    // 계약 문서 임시 저장
    @Transactional
    @ParticipantCheck
    public void saveDraft(Integer contractId, Map<String, Object> request) {

        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        // Negotiating_A,B일때만 임시저장 가능
        contractValidator.validateStatus(
                contract,
                ContractStatus.NEGOTIATING_A,
                ContractStatus.NEGOTIATING_B
        );

        if (contract.getStatus() == ContractStatus.NEGOTIATING_A) {
            contractValidator.validateCompany(contract);// A상태는 회사만 가능
        } else {
            contractValidator.validateFreelancer(contract); //B상태는 사원만 가능
        }
        //테이블에 저장해놓기
        ContractDocumentDTO document = objectMapper.convertValue(request, ContractDocumentDTO.class);
        document.setContractId(contractId);
        contractDocumentMapper.update(document);
    }
    
    
    //==================================================================== 상태 관련
    
    // PROPOSAL : 회사는 제안을 보내고 아직 프리랜서가 수락안함
    @Transactional
    public Integer proposeFreelancer(Map<String, Object> request) {

        String companyEmail   = SecurityUtil.getCurrentEmail();
        Integer recruitmentId = (Integer) request.get("recruitmentId");
        String freelancerEmail = (String) request.get("freelancerEmail");

        
        // 공고 소유자 검증 - 본인 공고에 대해서만 계약 제안 가능
        RecruitmentDTO recruitment = recruitmentMapper.findById(recruitmentId);
        if (recruitment == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        if (!recruitment.getCompanyEmail().equals(companyEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        
        
        // 이미 진행 중인 계약 있으면 중복 생성 방지
        if (contractMapper.existsActiveContract(recruitmentId, freelancerEmail)) { throw new CustomException(ErrorCode.INVALID_INPUT);}

        //계약 db에 인서트
        ContractDTO dto = ContractDTO.builder()
                					 .recruitmentId(recruitmentId)
                					 .companyEmail(companyEmail)
                					 .freelancerEmail(freelancerEmail)
                					 .status(ContractStatus.PROPOSAL)
                					 .build();
        contractMapper.insert(dto);

        // 채팅방도 동시 생성
        ChatRoomDTO chatRoom = ChatRoomDTO.builder()
                						   .contractId(dto.getContractId())
                						   .companyEmail(companyEmail)
                						   .freelancerEmail(freelancerEmail)
                						   .build();
        chatRoomMapper.insert(chatRoom);

        // 프리랜서에게 알림
//        notificationService.createNotification(
//                freelancerEmail,
//                NotificationType.PROPOSAL,
//                dto.getContractId()
//        );
        return dto.getContractId(); 
    }
    
    
    //제안목록 조회
    public PageResponse<Map<String, Object>> getProposals(
            PageRequest pageRequest,
            String keywordType,
            String keyword,
            String sort) {

        String currentEmail = SecurityUtil.getCurrentEmail();

        Map<String, Object> param = new HashMap<>();
        param.put("email", currentEmail);
        param.put("keywordType", keywordType);
        param.put("keyword", keyword);
        param.put("sort", sort);
        param.put("offset", pageRequest.getOffset());
        param.put("size", pageRequest.getSize());

        long total = contractMapper.countProposals(param);
        List<Map<String, Object>> list = contractMapper.searchProposals(param);

        return PageResponse.of(list, total, pageRequest);
    }
    
    
    // Proposal -> Waiting
    //회사의 제안 프리랜서가 받아봄 : 계약 제안 시작 (WAITING 삽입)
    @Transactional
    @ParticipantCheck
    public void acceptProposal(Integer contractId) {

        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        contractValidator.validateFreelancer(contract);

        
        if (contract.getStatus() != ContractStatus.PROPOSAL) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        contractMapper.updateStatus(
                contractId,
                ContractStatus.WAITING
        );

        notificationService.createNotification(
                contract.getCompanyEmail(),
                NotificationType.PROPOSAL,
                contractId
        );
    }
    
    
 // 회사가 지원 수락 시 한번에 처리
 // WAITING으로 바로 생성 + 채팅방 + 공고마감 + 지원서연결
 @Transactional
 public Integer acceptApplicationByCompany(Map<String, Object> request) {

     String companyEmail    = SecurityUtil.getCurrentEmail();
     Integer recruitmentId  = (Integer) request.get("recruitmentId");
     String freelancerEmail = (String)  request.get("freelancerEmail");
     Integer applicationId  = (Integer) request.get("applicationId");

     RecruitmentDTO recruitment = recruitmentMapper.findById(recruitmentId);
     if (recruitment == null) throw new CustomException(ErrorCode.NOT_FOUND);
     if (!recruitment.getCompanyEmail().equals(companyEmail)) throw new CustomException(ErrorCode.FORBIDDEN);

     if (contractMapper.existsActiveContract(recruitmentId, freelancerEmail)) {
         throw new CustomException(ErrorCode.INVALID_INPUT);
     }

     // 계약 WAITING으로 바로 생성
     ContractDTO dto = ContractDTO.builder()
                                  .recruitmentId(recruitmentId)
                                  .companyEmail(companyEmail)
                                  .freelancerEmail(freelancerEmail)
                                  .status(ContractStatus.WAITING)
                                  .build();
     contractMapper.insert(dto);

     // 채팅방 생성
     ChatRoomDTO chatRoom = ChatRoomDTO.builder()
                                       .contractId(dto.getContractId())
                                       .companyEmail(companyEmail)
                                       .freelancerEmail(freelancerEmail)
                                       .build();
     chatRoomMapper.insert(chatRoom);

     // 공고 마감
     recruitmentMapper.closeIfOpen(recruitmentId);

     // 지원서에 contractId 연결
     applicationMapper.attachContract(applicationId, dto.getContractId());
     applicationMapper.updateStatusIfPending(applicationId, ApplicationStatus.ACCEPTED);


     return dto.getContractId();
 }
    

    // WAITING -> NEGOTIATING_A
    @Transactional
    @ParticipantCheck
    public void startContract(Integer contractId) {
    	// 계약유효성 검사
        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        contractValidator.validateCompany(contract);
        contractValidator.validateWaiting(contract);


        //dto 만들어서 아이디만 넣어서 빈껍대기 row 인서트 해놓기
        ContractDocumentDTO document = ContractDocumentDTO.builder()
                										  .contractId(contractId)
                										  .build();
        contractDocumentMapper.insert(document);
        //상태 변경
        contractMapper.updateStatus(contractId, ContractStatus.NEGOTIATING_A);
    }



    // NEGOTIATING_A -> NEGOTIATING_B
    @Transactional
    @ParticipantCheck
    public void sendByCompany(Integer contractId, Map<String, Object> request) {
    	// 계약유효성 검사
        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        contractValidator.validateCompany(contract);
        contractValidator.validateNegotiatingA(contract);

        //들어온 값가지고 document 디티오 만들어서 테이블 업데이트
        ContractDocumentDTO document = objectMapper.convertValue(request, ContractDocumentDTO.class);
        document.setContractId(contractId);
        contractDocumentMapper.update(document);

        //상태 변경
        contractMapper.updateStatus(contractId, ContractStatus.NEGOTIATING_B);
        //알림 발송
        // 발송 행위 자체가 이전 PROPOSAL 알림에 대한 대응이므로 발송자(회사) 측 알림 클리어
        notificationService.markSpecificTypeAsRead(
                contract.getCompanyEmail(), contractId, NotificationType.PROPOSAL);

        notificationService.createNotification(
                contract.getFreelancerEmail(),
                NotificationType.PROPOSAL,
                contractId
        );
    }


    // NEGOTIATING_B -> NEGOTIATING_C
    @Transactional
    @ParticipantCheck
    public void sendByFreelancer(Integer contractId, Map<String, Object> request) {
    	// 계약유효성 검사
        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        contractValidator.validateFreelancer(contract);
        contractValidator.validateNegotiatingB(contract);
        
        //들어온 값가지고 document 디티오 만들어서 테이블 업데이트
        ContractDocumentDTO document = objectMapper.convertValue(request, ContractDocumentDTO.class);
        document.setContractId(contractId);
        contractDocumentMapper.update(document);
        
        //상태 변경
        contractMapper.updateStatus(contractId, ContractStatus.NEGOTIATING_C);
        //알림 발송
        notificationService.markSpecificTypeAsRead(
                contract.getFreelancerEmail(), contractId, NotificationType.PROPOSAL);

        notificationService.createNotification(
                contract.getCompanyEmail(),
                NotificationType.PROPOSAL,
                contractId
        );
    }

    
    

    // NEGOTIATING_C -> IN_PROGRESS
    @Transactional
    @ParticipantCheck
    public void approveContract(Integer contractId) {
    	// 계약유효성 검사
        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        contractValidator.validateCompany(contract);
        contractValidator.validateNegotiatingC(contract);
        
        // 계약서 데이터 존재 여부 확인
        ContractDocumentDTO document = contractDocumentMapper.findByContractId(contractId);
        if (document == null) { throw new CustomException(ErrorCode.NOT_FOUND);}
        
        // 이미 생성된 PDF 존재 여부 확인 (방어 코드)
        ContractFileDTO existingPdf =contractFileMapper.findPdfByContractId(contractId);
        if (existingPdf != null) { throw new CustomException(ErrorCode.INVALID_INPUT); }
        
        //pdf를 byte[]로 생성
        byte[] pdfBytes =contractPdfService.generateContractPdf(document);
        //멀티파트 형시으로 업글
        MultipartFile pdfMultipart =
                new BytesMultipartFile(
                        "contract_" + contractId + ".pdf",
                        "contract_" + contractId + ".pdf",
                        "application/pdf",
                        pdfBytes
                );

        // GCS 업로드 및 file 테이블 저장
        FileDTO savedFile =fileService.uploadContractPdf(pdfMultipart,contractId, contract.getCompanyEmail()); 
        
        // contract_file 테이블에 PDF 연결 정보 인서트
        ContractFileDTO pdfFile = ContractFileDTO.builder()
                        						 .contractId(contractId)
                        						 .fileId(savedFile.getFileId())
                        						 .type(ContractFileType.PDF)
                        						 .build();
        contractFileMapper.insert(pdfFile); 
        contractDocumentMapper.updateConfirmedAt(contractId); // contractDocumentMapper에 확정일자 작성
        //상태 변경
        contractMapper.updateStatus( // 상태변경
                contractId,
                ContractStatus.IN_PROGRESS
        );
    }



    // COMPLETED_PENDING -> COMPLETED
    @Transactional
    @ParticipantCheck
    public void completeContract(Integer contractId) {
    	// 계약유효성 검사
        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        contractValidator.validateCompany(contract);
        contractValidator.validateCompletedPending(contract);
        
        //상태 변경
        contractMapper.updateStatus(contractId, ContractStatus.COMPLETED);
        //알림 발송
        notificationService.createNotification(
                contract.getFreelancerEmail(),
                NotificationType.CONTRACT_COMPLETED,
                contractId
        );
    }


  //==================================================================== 계약 파기 요청-> 계약 파기

    // 계약 파기 요청
    @Transactional
    @ParticipantCheck
    public void requestCancel(Integer contractId) {
    	// 취소가능한 상태의 계약인지 유효성 검사
        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        contractValidator.validateCancelable(contract);

        //이미 파기 요청한 계약인지 확인하기
        if (contractCancelRequestMapper.existsByContractId(contractId)) { throw new CustomException(ErrorCode.INVALID_INPUT);}

        String requesterEmail = SecurityUtil.getCurrentEmail();

        //계약파기 요청
        ContractCancelRequestDTO dto = ContractCancelRequestDTO.builder()
                											   .contractId(contractId)
                											   .requesterEmail(requesterEmail)
                											   .build();
        contractCancelRequestMapper.insert(dto);

        //상대측에 알림 발송
        String targetEmail = requesterEmail.equals(contract.getCompanyEmail())
                ? contract.getFreelancerEmail()
                : contract.getCompanyEmail();
        notificationService.createNotification(
                targetEmail,
                NotificationType.CONTRACT_CANCEL_REQUEST,
                contractId
        );
    }

    // 계약 파기
    @Transactional
    @ParticipantCheck
    public void cancelContract(Integer contractId) {

        ContractDTO contract = contractValidator.getContractOrThrow(contractId);
        contractValidator.validateCancelable(contract);

        // 파기 요청 없으면 파기 불가
        if (!contractCancelRequestMapper.existsByContractId(contractId)) {throw new CustomException(ErrorCode.INVALID_INPUT);}

        // 요청 받은 사람만 파기 가능
        String currentEmail = SecurityUtil.getCurrentEmail();
        String requesterEmail = contractCancelRequestMapper.findRequesterEmail(contractId);
        if (currentEmail.equals(requesterEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN); // 요청자는 직접 파기 불가 — 이건 맞음
        }

        // IN_PROGRESS라면 -> CANCELLED 로 상태변경임
    	// + 계약서 PDF 삭제
        if (contract.getStatus() == ContractStatus.IN_PROGRESS) {
        		deleteContractPdf(contractId);
        	    contractCancelRequestMapper.delete(contractId);
        	    contractMapper.updateStatus(contractId, ContractStatus.CANCELLED);

        } else {
            //완전 삭제 시 - PDF/컨펌파일 등 첨부파일도 정리 (소유자와 무관하게 시스템 삭제)
        	List<ContractFileDTO> files = contractFileMapper.findByContractId(contractId);
            for (ContractFileDTO file : files) {
                fileService.deleteBySystem(file.getFileId());
            }
            contractMapper.deleteContract(contractId);
        }
        // 실제 파기가 확정된 시점에만 CONTRACT_CANCEL_REQUEST 알림 제거 (양쪽 모두)
        notificationService.markSpecificTypeAsRead(
                contract.getCompanyEmail(), contractId, NotificationType.CONTRACT_CANCEL_REQUEST);
        notificationService.markSpecificTypeAsRead(
                contract.getFreelancerEmail(), contractId, NotificationType.CONTRACT_CANCEL_REQUEST);
        
    }
    
    //계약서 삭제
    private void deleteContractPdf(Integer contractId) {
        ContractFileDTO pdfFile =contractFileMapper.findPdfByContractId(contractId);
        if (pdfFile == null) {
            return;
        }
        contractFileMapper.delete(pdfFile.getContractFileId()); //
        fileService.deleteBySystem(pdfFile.getFileId());
    }
    
    
    
  //==================================================================== 계약서 pdf
    
    // PDF 다운로드 URL 조회
    @ParticipantCheck
    public Map<String, Object> getPdfDownloadUrl(Integer contractId) {

        ContractDTO contract = contractValidator.getContractOrThrow(contractId);

        contractValidator.validateStatus(
                contract,
                ContractStatus.IN_PROGRESS,
                ContractStatus.COMPLETED_PENDING,
                ContractStatus.COMPLETED
        );

        ContractFileDTO pdfFile = contractFileMapper.findPdfByContractId(contractId);
        if (pdfFile == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        String downloadUrl = fileService.getDownloadUrl(pdfFile.getFileId());

        Map<String, Object> result = new HashMap<>();
        result.put("contractFileId", pdfFile.getContractFileId());
        result.put("fileId", pdfFile.getFileId());
        result.put("downloadUrl", downloadUrl);

        return result;
    }
    
    
  //==================================================================== 컨펌 파일

    
}