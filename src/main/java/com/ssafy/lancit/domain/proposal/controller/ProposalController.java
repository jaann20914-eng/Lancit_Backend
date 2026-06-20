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

//제안서 = 회사가 프리랜서에게 특정 공고로 제안
//제안 즉시 Contract(NEGOTIATING_A) + ChatRoom 생성
//STOMP: 제안 시 /sub/notification/{freelancerEmail} 알림 푸시
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    /**
     * CLI-SEAR-02 제안 보내기 (회사 전용)
     * - 공고문 선택 후 프리랜서에게 제안
     * - 제안 즉시 Contract(NEGOTIATING_A) + ChatRoom 생성
     * - 공고문 디테일 페이지에서도 동일한 엔드포인트 사용
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 companyEmail 꺼내기
     * TODO 지원 [2]: proposalService.send(dto, companyEmail) 호출
     *               → Contract(NEGOTIATING_A) INSERT
     *               → ChatRoom INSERT
     *               → STOMP /sub/notification/{freelancerEmail} 알림 발송
     * TODO 지원 [3]: 생성된 chatRoomId 포함 ProposalDTO 반환
     *               → 프론트가 chatRoomId 받아서 채팅방으로 이동
     */
//    @PostMapping
//    public ResponseEntity<ApiResponse<ProposalDTO>> sendProposal(
//            @RequestBody ProposalDTO dto) {
//    	String companyEmail= SecurityUtil.getCurrentEmail();
//    	ProposalDTO proposaldto = proposalService.send(dto, companyEmail);
//        return ResponseEntity.ok(ApiResponse.ok(proposaldto));
//    }
}