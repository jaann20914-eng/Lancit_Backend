package com.ssafy.lancit.domain.proposal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.domain.proposal.service.ProposalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    /**
     * PROP-01 제안서 목록 조회
     * - 프리랜서(USER)  → 받은 제안서 목록
     * - 회사(COMPANY)   → 보낸 제안서 목록
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 이메일 꺼내기
     * TODO 지원 [2]: SecurityUtil.getCurrentRole() 로 role 꺼내기
     * TODO 지원 [3]: proposalService.getList(email, role) 호출
     * TODO 지원 [4]: ApiResponse.ok(list) 반환
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProposalDTO>>> getProposals() {
        // TODO 지원 [1] ~ [4] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * PROP-02 제안서 상세 조회
     *
     * TODO 지원 [1]: proposalService.getOne(proposalId) 호출
     * TODO 지원 [2]: ApiResponse.ok(proposalDTO) 반환
     */
    @GetMapping("/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalDTO>> getProposal(@PathVariable int proposalId) {
        // TODO 지원 [1] ~ [2] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * CLI-SEAR-02 / CLI-APPLY-03 회사가 제안서 발송
     * - 회사(COMPANY) 전용
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 companyEmail 꺼내기
     * TODO 지원 [2]: proposalService.send(dto, companyEmail) 호출
     * TODO 지원 [3]: ApiResponse.ok(null) 반환
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendProposal(@RequestBody ProposalDTO dto) {
        // TODO 지원 [1] ~ [3] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * PROP-03 프리랜서가 제안서 수락
     * - 3개 테이블 동시 처리 (트랜잭션)
     *   1. Proposal status → ACCEPTED
     *   2. Contract INSERT (status = NEGOTIATING_A)
     *   3. ChatRoom INSERT
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 freelancerEmail 꺼내기
     * TODO 지원 [2]: proposalService.accept(proposalId, freelancerEmail) 호출
     * TODO 지원 [3]: ApiResponse.ok(null) 반환
     */
    @PutMapping("/{proposalId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptProposal(@PathVariable int proposalId) {
        // TODO 지원 [1] ~ [3] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * PROP-04 프리랜서가 제안서 거절
     * - Proposal status → REJECTED
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 freelancerEmail 꺼내기
     * TODO 지원 [2]: proposalService.reject(proposalId, freelancerEmail) 호출
     * TODO 지원 [3]: ApiResponse.ok(null) 반환
     */
    @PutMapping("/{proposalId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectProposal(@PathVariable int proposalId) {
        // TODO 지원 [1] ~ [3] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}