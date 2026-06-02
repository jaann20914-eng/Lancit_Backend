package com.ssafy.lancit.domain.contract.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.contract.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.service.ContractService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 계약서 CRUD + PDF 다운로드 + 채팅방 조회
// 서명 이미지 → Redis Base64 캐싱 (ContractPdfService 에서 처리)
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    // CONT-01 / CLI-CONT-01 계약서 목록 조회 (상태/키워드 필터 + 페이지네이션)
    // role 로 USER(freelancerEmail) / COMPANY(companyEmail) 분기
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ContractDTO>>> getContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @ModelAttribute PageRequest pageRequest) {
        // TODO 지원 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: String role = SecurityUtil.getCurrentRole()
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(
        //               contractService.getList(email, role, status, keyword, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CONT-07 계약서 상세 조회
    // 서명 파일 3종 (representativeSignFileId, freelancerSignFileId, confirmSignFileId) 포함
    // Signed URL 은 프론트가 /api/files/{fileId}/url 로 별도 호출
    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractDTO>> getContract(@PathVariable int contractId) {
        // TODO 지원 [1]: return ResponseEntity.ok(ApiResponse.ok(contractService.getOne(contractId)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CONT-06 (회사) 계약서 작성 및 전송
    // contractId 없으면 최초 작성(INSERT), 있으면 수정(UPDATE)
    // status 흐름: NEGOTIATING_A → B → C
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createOrUpdateContract(@RequestBody ContractDTO dto) {
        // TODO 지원 [1]: String companyEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: contractService.createOrUpdate(dto, companyEmail)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CONT-06 (프리랜서) 계약서 수락
    // status → IN_PROGRESS, freelancerSignFileId 업데이트
    @PutMapping("/{contractId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptContract(@PathVariable int contractId,
                                                            @RequestBody ContractDTO dto) {
        // TODO 지원 [1]: String freelancerEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: contractService.accept(contractId, dto, freelancerEmail)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CONT-08 계약 파기 - 프리랜서/회사 양측 모두 가능
    // status → CANCELLED
    @PutMapping("/{contractId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelContract(@PathVariable int contractId) {
        // TODO 지원 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: contractService.cancel(contractId, email)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CONT-02 채팅방 조회 - chatRoomId 얻기 위해 채팅 페이지 진입 시 호출
    @GetMapping("/{contractId}/chatroom")
    public ResponseEntity<ApiResponse<ChatRoomDTO>> getChatRoom(@PathVariable int contractId) {
        // TODO 지원 [1]: return ResponseEntity.ok(ApiResponse.ok(contractService.getChatRoom(contractId)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CONT-07 계약서 PDF 다운로드
    // ★ 서명 이미지 Base64 는 Redis 에서 캐싱 (ContractPdfService 에서 처리)
    // IN_PROGRESS / COMPLETED 상태만 허용
    @GetMapping("/{contractId}/pdf")
    public void downloadPdf(@PathVariable int contractId,
                            HttpServletResponse response) throws Exception {
        // TODO 지원 [1]: ContractDTO dto = contractService.getOne(contractId)
        // TODO 지원 [2]: status 가 IN_PROGRESS / COMPLETED 아니면
        //               throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [3]: response.setContentType("application/pdf")
        //               response.setHeader("Content-Disposition",
        //                   "attachment; filename=contract_" + contractId + ".pdf")
        // TODO 지원 [4]: contractPdfService.generatePdf(dto, response.getOutputStream())
    }
}