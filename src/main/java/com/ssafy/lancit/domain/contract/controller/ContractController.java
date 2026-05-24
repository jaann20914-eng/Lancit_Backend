package com.ssafy.lancit.domain.contract.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.service.ContractService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {
 
    private final ContractService contractService;
 
    /** CONT-01 / CLI-CONT-01 계약서 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContractDTO>>> getContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        // TODO 지원: contractService.getList(email, role, status, keyword)
        //   role=USER → freelancerEmail 필터, role=COMPANY → companyEmail 필터
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CONT-07 계약서 상세 조회 */
    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractDTO>> getContract(@PathVariable int contractId) {
        // TODO 지원: contractService.getOne(contractId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CONT-06 (회사) 계약서 작성/전송
     *  - status: NEGOTIATING_A → B → C 흐름
     *  - representativeSignFileId 업데이트 */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createOrUpdateContract(@RequestBody ContractDTO dto) {
        // TODO 지원: contractService.createOrUpdate(dto, companyEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CONT-06 (프리랜서) 계약서 수락 → IN_PROGRESS
     *  - freelancerSignFileId 업데이트 */
    @PutMapping("/{contractId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptContract(@PathVariable int contractId,
                                                            @RequestBody ContractDTO dto) {
        // TODO 지원: contractService.accept(contractId, dto, freelancerEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CONT-08 계약 파기 → CANCELLED */
    @PutMapping("/{contractId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelContract(@PathVariable int contractId) {
        // TODO 지원: contractService.cancel(contractId, email)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CONT-02 계약서의 채팅방 조회 */
    @GetMapping("/{contractId}/chatroom")
    public ResponseEntity<ApiResponse<?>> getChatRoom(@PathVariable int contractId) {
        // TODO 지원: contractService.getChatRoom(contractId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}