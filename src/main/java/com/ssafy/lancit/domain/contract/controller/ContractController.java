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
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.service.ContractService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    /**
     * CONT-01 (프리랜서) / CLI-CONT-01 (회사) 계약서 목록 조회
     * - 상태 필터, 키워드 검색, 기간 정렬 지원
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 이메일 꺼내기
     * TODO 지원 [2]: SecurityUtil.getCurrentRole() 로 role 꺼내기
     *               - "USER"    → freelancerEmail 기준 조회
     *               - "COMPANY" → companyEmail 기준 조회
     * TODO 지원 [3]: contractService.getList(email, role, status, keyword) 호출
     * TODO 지원 [4]: ApiResponse.ok(list) 반환
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContractDTO>>> getContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        // TODO 지원 [1] ~ [4] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * CONT-07 계약서 상세 조회
     * - 서명 파일 3종 (representativeSignFileId, freelancerSignFileId, confirmSignFileId) 포함
     *
     * TODO 지원 [1]: contractService.getOne(contractId) 호출
     * TODO 지원 [2]: ApiResponse.ok(dto) 반환
     */
    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractDTO>> getContract(@PathVariable int contractId) {

        // TODO 지원 [1] ~ [2] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * CONT-06 (회사) 계약서 작성 및 전송
     * - 최초 작성: Contract INSERT (status = NEGOTIATING_A)
     * - 이후 수정: status NEGOTIATING_A → B → C 순서로 UPDATE
     * - 회사 서명 파일(representativeSignFileId) 업데이트
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 companyEmail 꺼내기
     * TODO 지원 [2]: contractService.createOrUpdate(dto, companyEmail) 호출
     * TODO 지원 [3]: ApiResponse.ok(null) 반환
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createOrUpdateContract(@RequestBody ContractDTO dto) {

        // TODO 지원 [1] ~ [3] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * CONT-06 (프리랜서) 계약서 수락
     * - status → IN_PROGRESS 로 변경
     * - 프리랜서 서명 파일(freelancerSignFileId) 업데이트
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 freelancerEmail 꺼내기
     * TODO 지원 [2]: contractService.accept(contractId, dto, freelancerEmail) 호출
     * TODO 지원 [3]: ApiResponse.ok(null) 반환
     */
    @PutMapping("/{contractId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptContract(@PathVariable int contractId,
                                                            @RequestBody ContractDTO dto) {

        // TODO 지원 [1] ~ [3] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * CONT-08 계약 파기
     * - status → CANCELLED 로 변경
     * - 프리랜서 / 회사 양측 모두 파기 가능
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 이메일 꺼내기
     * TODO 지원 [2]: contractService.cancel(contractId, email) 호출
     *               - 서비스 내부에서 해당 계약의 companyEmail / freelancerEmail 중
     *                 하나와 일치하는지 검증 후 파기 처리
     * TODO 지원 [3]: ApiResponse.ok(null) 반환
     */
    @PutMapping("/{contractId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelContract(@PathVariable int contractId) {

        // TODO 지원 [1] ~ [3] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * CONT-02 계약서 채팅방 조회
     * - 계약서 ID 로 연결된 ChatRoom 조회
     * - 채팅 페이지 진입 시 chatRoomId 얻기 위해 호출
     *
     * TODO 지원 [1]: contractService.getChatRoom(contractId) 호출
     * TODO 지원 [2]: ApiResponse.ok(chatRoomDTO) 반환
     *               - 반환 타입 ApiResponse<?> → ApiResponse<ChatRoomDTO> 로 변경 권장
     */
    @GetMapping("/{contractId}/chatroom")
    public ResponseEntity<ApiResponse<?>> getChatRoom(@PathVariable int contractId) {

        // TODO 지원 [1] ~ [2] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}