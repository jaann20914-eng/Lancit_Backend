package com.ssafy.lancit.domain.proposal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.contract.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.domain.proposal.mapper.ProposalMapper;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.ProposalStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalMapper proposalMapper;
    private final ContractMapper contractMapper;
    private final ChatRoomMapper chatRoomMapper;

    /**
     * PROP-01 제안서 목록 조회
     *
     * TODO 지원 [1]: role 분기
     *               - "USER"    → proposalMapper.findByFreelancer(email)
     *               - "COMPANY" → proposalMapper.findByCompany(email)
     * TODO 지원 [2]: 조회된 List<ProposalDTO> 반환
     */
    public List<ProposalDTO> getList(String email, String role) {
        // TODO 지원 [1] ~ [2] 구현
        return null;
    }

    /**
     * PROP-02 제안서 상세 조회
     *
     * TODO 지원 [1]: proposalMapper.findById(proposalId) 호출
     * TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [3]: 조회된 ProposalDTO 반환
     */
    public ProposalDTO getOne(int proposalId) {
        // TODO 지원 [1] ~ [3] 구현
        return null;
    }

    /**
     * CLI-SEAR-02 / CLI-APPLY-03 제안서 발송
     *
     * TODO 지원 [1]: dto.setCompanyEmail(companyEmail) 세팅
     * TODO 지원 [2]: dto.setStatus(ProposalStatus.PENDING) 세팅
     * TODO 지원 [3]: proposalMapper.insert(dto) 호출
     * TODO 지원 [4]: 알림 발송
     *               - messagingTemplate.convertAndSend(
     *                   "/sub/notification/" + dto.getFreelancerEmail(),
     *                   NotificationDTO(type=PROPOSAL, message=...))
     */
    @Transactional
    public void send(ProposalDTO dto, String companyEmail) {
        // TODO 지원 [1] ~ [4] 구현
    }

    /**
     * PROP-03 제안서 수락
     * - 3개 테이블 동시 처리 → 하나라도 실패 시 전체 롤백
     *
     * TODO 지원 [1]: proposalMapper.findById(proposalId) 호출
     *               - null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [2]: proposal.getFreelancerEmail() 과 freelancerEmail 일치 확인
     *               - 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
     * TODO 지원 [3]: proposalMapper.updateStatus(proposalId, ProposalStatus.ACCEPTED) 호출
     * TODO 지원 [4]: ContractDTO 만들어서 Contract INSERT
     *               - contractDto.setCompanyEmail(proposal.getCompanyEmail())
     *               - contractDto.setFreelancerEmail(freelancerEmail)
     *               - contractDto.setStatus(ContractStatus.NEGOTIATING_A)
     *               - contractMapper.insert(contractDto)
     * TODO 지원 [5]: ChatRoomDTO 만들어서 ChatRoom INSERT
     *               - chatRoomDto.setContractId(contractDto.getContractId())
     *               - chatRoomDto.setFreelancerEmail(freelancerEmail)
     *               - chatRoomDto.setCompanyEmail(proposal.getCompanyEmail())
     *               - chatRoomMapper.insert(chatRoomDto)
     */
    @Transactional
    public void accept(int proposalId, String freelancerEmail) {
        // TODO 지원 [1] ~ [5] 구현
    }

    /**
     * PROP-04 제안서 거절
     *
     * TODO 지원 [1]: proposalMapper.findById(proposalId) 호출
     *               - null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [2]: proposal.getFreelancerEmail() 과 freelancerEmail 일치 확인
     *               - 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
     * TODO 지원 [3]: proposalMapper.updateStatus(proposalId, ProposalStatus.REJECTED) 호출
     */
    @Transactional
    public void reject(int proposalId, String freelancerEmail) {
        // TODO 지원 [1] ~ [3] 구현
    }
}