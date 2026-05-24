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
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.domain.proposal.service.ProposalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {
 
    private final ProposalService proposalService;
 
    /** PROP-01 제안서 목록 조회 (프리랜서: 받은 것 / 회사: 보낸 것) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProposalDTO>>> getProposals() {
        // TODO 지원: proposalService.getList(email, role)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** PROP-02 제안서 상세 조회 */
    @GetMapping("/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalDTO>> getProposal(@PathVariable int proposalId) {
        // TODO 지원: proposalService.getOne(proposalId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-SEAR-02 / CLI-APPLY-03 회사가 제안서 발송 */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendProposal(@RequestBody ProposalDTO dto) {
        // TODO 지원: proposalService.send(dto, companyEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** PROP-03 프리랜서가 제안서 수락
     *  → Proposal ACCEPTED + Contract(NEGOTIATING_A) 삽입 + ChatRoom 삽입 (트랜잭션) */
    @PutMapping("/{proposalId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptProposal(@PathVariable int proposalId) {
        // TODO 지원: proposalService.accept(proposalId, freelancerEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** PROP-04 프리랜서가 제안서 거절 */
    @PutMapping("/{proposalId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectProposal(@PathVariable int proposalId) {
        // TODO 지원: proposalService.reject(proposalId, freelancerEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}