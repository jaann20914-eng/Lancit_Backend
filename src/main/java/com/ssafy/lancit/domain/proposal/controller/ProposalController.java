package com.ssafy.lancit.domain.proposal.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.domain.proposal.service.ProposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 제안서 CRUD - 회사(발송), 프리랜서(수락/거절)
// ★ 제안서 발송 시 STOMP /sub/notification/{freelancerEmail} 로 알림 푸시 (ProposalService 에서 처리)
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    // PROP-01 제안서 목록 조회 (페이지네이션)
    // role 로 USER(받은 목록) / COMPANY(보낸 목록) 분기
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProposalDTO>>> getProposals(
            @ModelAttribute PageRequest pageRequest) {
        // TODO 지원 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: String role = SecurityUtil.getCurrentRole()
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(
        //               proposalService.getList(email, role, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PROP-02 제안서 상세 조회
    @GetMapping("/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalDTO>> getProposal(@PathVariable int proposalId) {
        // TODO 지원 [1]: return ResponseEntity.ok(ApiResponse.ok(proposalService.getOne(proposalId)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-SEAR-02 제안서 발송 (회사 전용)
    // ★ STOMP: 발송 후 프리랜서에게 /sub/notification/{freelancerEmail} 알림 푸시
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendProposal(@RequestBody ProposalDTO dto) {
        // TODO 지원 [1]: String companyEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: proposalService.send(dto, companyEmail)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PROP-03 제안서 수락 (프리랜서 전용)
    // ★ 수락 시 Contract + ChatRoom 동시 INSERT (트랜잭션)
    @PutMapping("/{proposalId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptProposal(@PathVariable int proposalId) {
        // TODO 지원 [1]: String freelancerEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: proposalService.accept(proposalId, freelancerEmail)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PROP-04 제안서 거절 (프리랜서 전용)
    @PutMapping("/{proposalId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectProposal(@PathVariable int proposalId) {
        // TODO 지원 [1]: String freelancerEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: proposalService.reject(proposalId, freelancerEmail)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}