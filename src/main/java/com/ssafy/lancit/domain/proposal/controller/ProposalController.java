package com.ssafy.lancit.domain.proposal.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.proposal.dto.ProposalCreateRequest;
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.domain.proposal.dto.ProposalStatusUpdateRequest;
import com.ssafy.lancit.domain.proposal.service.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Proposals", description = "회사가 프리랜서에게 보내는 제안 API")
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @Operation(summary = "제안 보내기", description = "회사 토큰과 회사 소유 공고가 필요합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ProposalDTO>> sendProposal(
            @Valid @RequestBody ProposalCreateRequest request) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(proposalService.send(request, email, role)));
    }

    @Operation(summary = "받은 제안 목록 조회", description = "프리랜서 토큰이 필요합니다.")
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<PageResponse<ProposalDTO>>> getReceivedProposals(
            @ModelAttribute PageRequest pageRequest) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(proposalService.getReceived(email, role, pageRequest)));
    }

    @Operation(summary = "보낸 제안 목록 조회", description = "회사 토큰이 필요합니다.")
    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<PageResponse<ProposalDTO>>> getSentProposals(
            @ModelAttribute PageRequest pageRequest) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(proposalService.getSent(email, role, pageRequest)));
    }

    @Operation(summary = "제안 상세 조회", description = "제안을 보낸 회사 또는 받은 프리랜서만 조회할 수 있습니다.")
    @GetMapping("/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalDTO>> getProposal(@PathVariable int proposalId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(proposalService.getOne(proposalId, email, role)));
    }

    @Operation(summary = "받은 제안 수락/거절", description = "PENDING 제안의 수신 프리랜서만 처리할 수 있습니다.")
    @PatchMapping("/{proposalId}/status")
    public ResponseEntity<ApiResponse<ProposalDTO>> updateProposalStatus(
            @PathVariable int proposalId,
            @Valid @RequestBody ProposalStatusUpdateRequest request) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                proposalService.updateStatus(proposalId, request, email, role)));
    }
}
