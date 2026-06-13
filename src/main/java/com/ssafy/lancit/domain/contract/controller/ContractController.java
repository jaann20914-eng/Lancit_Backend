package com.ssafy.lancit.domain.contract.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDocumentDTO;
import com.ssafy.lancit.domain.contract.dto.ContractFileDTO;
import com.ssafy.lancit.domain.contract.service.ContractFileService;
import com.ssafy.lancit.domain.contract.service.ContractPdfService;
import com.ssafy.lancit.domain.contract.service.ContractService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ContractFileService contractFileService;
    private final ContractPdfService contractPdfService;

  //==================================================================== 조회 관련 + 임시저장
    // 계약 목록 조회
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> getContracts(
            @ModelAttribute PageRequest pageRequest,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keywordType,
            @RequestParam(required = false) String keyword) {

        return ApiResponse.ok(
                contractService.getContracts(pageRequest, status, keywordType, keyword)
        );
    }


    // 계약 상세 조회
    @GetMapping("/{contractId}")
    public ApiResponse<Map<String, Object>> getContractDetail(
            @PathVariable Integer contractId) {

        return ApiResponse.ok(
                contractService.getContractDetail(contractId)
        );
    }


    // 계약 문서 임시 저장
    @PutMapping("/{contractId}/draft")
    public ApiResponse<Void> saveDraft(
            @PathVariable Integer contractId,
            @RequestBody Map<String, Object> request) {

        contractService.saveDraft(contractId, request);
        return ApiResponse.ok(null);
    }
    
    // 신규: 제안 거절 엔드포인트 추가
    // 제안 거절 (WAITING -> 완전 삭제, 프리랜서 전용) - Image2 "거절하기"
    @PutMapping("/{contractId}/reject")
    public ApiResponse<Void> rejectContract(@PathVariable Integer contractId) {
        contractService.rejectContract(contractId);
        return ApiResponse.ok(null);
    }
    
    
    //==================================================================== 상태 관련
    
    // 계약 생성 (WAITING 삽입)
	 // 회사가 프리랜서에게 계약 시작 버튼 누를 때
	 @PostMapping
	 public ApiResponse<Void> createContract( @RequestBody Map<String, Object> request) {
	     contractService.createContract(request);
	     return ApiResponse.ok(null);
	 }


    // 계약 작성 시작 (WAITING -> NEGOTIATING_A)
    @PutMapping("/{contractId}/start")
    public ApiResponse<Void> startContract(@PathVariable Integer contractId) {
        contractService.startContract(contractId);
        return ApiResponse.ok(null);
    }


    // 회사 계약서 발송 (NEGOTIATING_A -> NEGOTIATING_B)
    @PutMapping("/{contractId}/send-company")
    public ApiResponse<Void> sendByCompany( @PathVariable Integer contractId,
            								@RequestBody Map<String, Object> request) {
        contractService.sendByCompany(contractId, request);
        return ApiResponse.ok(null);
    }


    // 프리랜서 계약서 발송 (NEGOTIATING_B -> NEGOTIATING_C)
    @PutMapping("/{contractId}/send-freelancer")
    public ApiResponse<Void> sendByFreelancer( @PathVariable Integer contractId,
            								   @RequestBody Map<String, Object> request) {
        contractService.sendByFreelancer(contractId, request);
        return ApiResponse.ok(null);
    }


    // 계약 최종 승인 (NEGOTIATING_C -> IN_PROGRESS)
    @PutMapping("/{contractId}/approve")
    public ApiResponse<Void> approveContract( @PathVariable Integer contractId) {
        contractService.approveContract(contractId);
        return ApiResponse.ok(null);
    }

    
    // 계약 완료 (COMPLETED_PENDING -> COMPLETED)
    @PutMapping("/{contractId}/complete")
    public ApiResponse<Void> completeContract(
            @PathVariable Integer contractId) {

        contractService.completeContract(contractId);
        return ApiResponse.ok(null);
    }
    
    
    //==================================================================== 계약 파기 요청-> 계약 파기
    // 계약 파기 요청
    @PostMapping("/{contractId}/cancel-request")
    public ApiResponse<Void> requestCancel(
            @PathVariable Integer contractId) {

        contractService.requestCancel(contractId);
        return ApiResponse.ok(null);
    }
    // 계약 파기
    @PutMapping("/{contractId}/cancel")
    public ApiResponse<Void> cancelContract(
            @PathVariable Integer contractId) {

        contractService.cancelContract(contractId);
        return ApiResponse.ok(null);
    }



  //==================================================================== 계약서 pdf

    // 저장된 PDF 다운로드 URL 조회
    @GetMapping("/{contractId}/pdf")
    public ApiResponse<Map<String, Object>> getPdfDownloadUrl(
            @PathVariable Integer contractId) {

        return ApiResponse.ok(
                contractService.getPdfDownloadUrl(contractId)
        );
    }

    // 내부 함수
    private String buildFilename(ContractDocumentDTO dto) {
    	String name = (dto.getPartyB() != null && !dto.getPartyB().isBlank())
    	        ? dto.getPartyB().trim()
    	        : "계약서";
        return "근로계약서_" + name + ".pdf";
    }
    
    
  //==================================================================== 컨펌 파일


    // 컨펌파일 업로드 (IN_PROGRESS, 프리랜서만)
    @PostMapping("/{contractId}/confirm-files")
    public ApiResponse<Void> uploadConfirmFile(
            @PathVariable Integer contractId,
            @RequestBody Map<String, Object> request) {

        Integer fileId = (Integer) request.get("fileId");
        contractFileService.uploadConfirmFile(contractId, fileId);
        return ApiResponse.ok(null);
    }


    // 컨펌파일 목록 조회
    @GetMapping("/{contractId}/confirm-files")
    public ApiResponse<List<ContractFileDTO>> getConfirmFiles(
            @PathVariable Integer contractId) {

        return ApiResponse.ok(
                contractFileService.getConfirmFiles(contractId)
        );
    }


    // 컨펌파일 삭제 (IN_PROGRESS, 프리랜서만)
    @DeleteMapping("/{contractId}/confirm-files/{contractFileId}")
    public ApiResponse<Void> deleteConfirmFile(
            @PathVariable Integer contractId,
            @PathVariable Integer contractFileId) {

        contractFileService.deleteConfirmFile(contractId, contractFileId);
        return ApiResponse.ok(null);
    }
}
