package com.ssafy.lancit.domain.proposal.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.contract.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.notification.dto.NotificationDTO;
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.domain.proposal.mapper.ProposalMapper;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.NotificationType;
import com.ssafy.lancit.global.enums.ProposalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 제안서 CRUD
// ★ STOMP: 제안서 발송 시 /sub/notification/{freelancerEmail} 으로 알림 푸시
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalMapper proposalMapper;
    private final ContractMapper contractMapper;
    private final ChatRoomMapper chatRoomMapper;
    private final SimpMessagingTemplate messagingTemplate;

    // PROP-01 제안서 목록 조회 (페이지네이션)
    // USER → 받은 목록, COMPANY → 보낸 목록
    public PageResponse<ProposalDTO> getList(String email, String role, PageRequest pageRequest) {
        List<ProposalDTO> list;
        long total;
        if ("USER".equals(role)) {
            list  = proposalMapper.findByFreelancer(email, pageRequest);
            total = proposalMapper.countByFreelancer(email);
        } else {
            list  = proposalMapper.findByCompany(email, pageRequest);
            total = proposalMapper.countByCompany(email);
        }
        return PageResponse.of(list, total, pageRequest);
    }

    // PROP-02 제안서 상세 조회
    public ProposalDTO getOne(int proposalId) {
        // TODO 지원 [1]: ProposalDTO dto = proposalMapper.findById(proposalId)
        // TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [3]: return dto
        return null;
    }

    // CLI-SEAR-02 제안서 발송 (회사 전용)
    // ★ STOMP: 발송 후 프리랜서에게 알림 푸시
    @Transactional
    public void send(ProposalDTO dto, String companyEmail) {
        // TODO 지원 [1]: dto.setCompanyEmail(companyEmail)
        // TODO 지원 [2]: dto.setStatus(ProposalStatus.PENDING)
        // TODO 지원 [3]: proposalMapper.insert(dto)
        // TODO 지원 [4]: ★ STOMP 알림 발송
        //               messagingTemplate.convertAndSend(
        //                   "/sub/notification/" + dto.getFreelancerEmail(),
        //                   NotificationDTO.builder()
        //                       .type(NotificationType.PROPOSAL)
        //                       .message("새 제안이 들어왔습니다.")
        //                       .build())
    }

    // PROP-03 제안서 수락 (프리랜서 전용)
    // ★ 트랜잭션: Proposal ACCEPTED + Contract INSERT + ChatRoom INSERT 동시 처리
    @Transactional
    public void accept(int proposalId, String freelancerEmail) {
        // TODO 지원 [1]: ProposalDTO proposal = proposalMapper.findById(proposalId)
        //               null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [2]: proposal.getFreelancerEmail() != freelancerEmail → throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [3]: proposalMapper.updateStatus(proposalId, ProposalStatus.ACCEPTED)
        // TODO 지원 [4]: Contract INSERT
        //               ContractDTO contract = new ContractDTO()
        //               contract.setCompanyEmail(proposal.getCompanyEmail())
        //               contract.setFreelancerEmail(freelancerEmail)
        //               contract.setStatus(ContractStatus.NEGOTIATING_A)
        //               contractMapper.insert(contract)
        // TODO 지원 [5]: ChatRoom INSERT
        //               ChatRoomDTO room = new ChatRoomDTO()
        //               room.setContractId(contract.getContractId())
        //               room.setFreelancerEmail(freelancerEmail)
        //               room.setCompanyEmail(proposal.getCompanyEmail())
        //               chatRoomMapper.insert(room)
    }

    // PROP-04 제안서 거절 (프리랜서 전용)
    @Transactional
    public void reject(int proposalId, String freelancerEmail) {
        // TODO 지원 [1]: ProposalDTO proposal = proposalMapper.findById(proposalId)
        //               null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [2]: proposal.getFreelancerEmail() != freelancerEmail → throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [3]: proposalMapper.updateStatus(proposalId, ProposalStatus.REJECTED)
    }
}