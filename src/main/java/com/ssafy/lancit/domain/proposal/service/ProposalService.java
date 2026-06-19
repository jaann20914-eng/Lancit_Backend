package com.ssafy.lancit.domain.proposal.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.proposal.dto.ProposalCreateRequest;
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.domain.proposal.dto.ProposalStatusUpdateRequest;
import com.ssafy.lancit.domain.proposal.mapper.ProposalMapper;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.ProposalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_COMPANY = "COMPANY";

    private final ProposalMapper proposalMapper;
    private final ContractMapper contractMapper;
    private final RecruitmentMapper recruitmentMapper;
    private final UserMapper userMapper;

    public PageResponse<ProposalDTO> getReceived(String freelancerEmail,
                                                 String role,
                                                 PageRequest pageRequest) {
        requireUser(role);
        List<ProposalDTO> proposals = proposalMapper.findByFreelancer(freelancerEmail, pageRequest);
        return PageResponse.of(proposals, proposalMapper.countByFreelancer(freelancerEmail), pageRequest);
    }

    public PageResponse<ProposalDTO> getSent(String companyEmail,
                                             String role,
                                             PageRequest pageRequest) {
        requireCompany(role);
        List<ProposalDTO> proposals = proposalMapper.findByCompany(companyEmail, pageRequest);
        return PageResponse.of(proposals, proposalMapper.countByCompany(companyEmail), pageRequest);
    }

    public ProposalDTO getOne(int proposalId, String email, String role) {
        ProposalDTO proposal = findProposal(proposalId);
        verifyParticipant(proposal, email, role);
        return proposal;
    }

    @Transactional
    public ProposalDTO send(ProposalCreateRequest request, String companyEmail, String role) {
        requireCompany(role);
        if (request == null || request.getRecruitmentId() == null
                || request.getFreelancerEmail() == null || request.getFreelancerEmail().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        RecruitmentDTO recruitment = recruitmentMapper.findById(request.getRecruitmentId());
        if (recruitment == null) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
        }
        if (!companyEmail.equals(recruitment.getCompanyEmail())) {
            throw new CustomException(ErrorCode.RECRUITMENT_FORBIDDEN);
        }

        String freelancerEmail = request.getFreelancerEmail().trim();
        UserDTO freelancer = userMapper.findByEmail(freelancerEmail);
        if (freelancer == null || freelancer.isDeleted()) {
            throw new CustomException(ErrorCode.PROPOSAL_FREELANCER_NOT_FOUND);
        }

        ProposalDTO proposal = ProposalDTO.builder()
                .companyEmail(companyEmail)
                .freelancerEmail(freelancerEmail)
                .recruitmentId(recruitment.getRecruitmentId())
                .status(ProposalStatus.PENDING)
                .build();
        proposalMapper.insert(proposal);
        return findProposal(proposal.getProposalId());
    }

    @Transactional
    public ProposalDTO updateStatus(int proposalId,
                                    ProposalStatusUpdateRequest request,
                                    String freelancerEmail,
                                    String role) {
        requireUser(role);
        ProposalStatus targetStatus = parseTargetStatus(request);
        ProposalDTO proposal = findProposal(proposalId);
        if (!freelancerEmail.equals(proposal.getFreelancerEmail())) {
            throw new CustomException(ErrorCode.PROPOSAL_FORBIDDEN);
        }
        if (!ProposalStatus.PENDING.equals(proposal.getStatus())) {
            throw new CustomException(ErrorCode.PROPOSAL_STATUS_CONFLICT);
        }
        if (ProposalStatus.ACCEPTED.equals(targetStatus)
                && contractMapper.existsActiveContract(proposal.getRecruitmentId(), freelancerEmail)) {
            throw new CustomException(ErrorCode.CONTRACT_ALREADY_EXISTS);
        }

        int updated = proposalMapper.updateStatusIfPending(proposalId, targetStatus);
        if (updated == 0) {
            throw new CustomException(ErrorCode.PROPOSAL_STATUS_CONFLICT);
        }

        if (ProposalStatus.ACCEPTED.equals(targetStatus)) {
            ContractDTO contract = ContractDTO.builder()
                    .recruitmentId(proposal.getRecruitmentId())
                    .companyEmail(proposal.getCompanyEmail())
                    .freelancerEmail(proposal.getFreelancerEmail())
                    .status(ContractStatus.WAITING)
                    .build();
            contractMapper.insert(contract);
            if (proposalMapper.attachContract(proposalId, contract.getContractId()) == 0) {
                throw new CustomException(ErrorCode.PROPOSAL_STATUS_CONFLICT);
            }
        }
        return findProposal(proposalId);
    }

    private ProposalDTO findProposal(int proposalId) {
        ProposalDTO proposal = proposalMapper.findById(proposalId);
        if (proposal == null) {
            throw new CustomException(ErrorCode.PROPOSAL_NOT_FOUND);
        }
        return proposal;
    }

    private void verifyParticipant(ProposalDTO proposal, String email, String role) {
        boolean allowed = (ROLE_USER.equals(role) && email.equals(proposal.getFreelancerEmail()))
                || (ROLE_COMPANY.equals(role) && email.equals(proposal.getCompanyEmail()));
        if (!allowed) {
            throw new CustomException(ErrorCode.PROPOSAL_FORBIDDEN);
        }
    }

    private ProposalStatus parseTargetStatus(ProposalStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PROPOSAL_STATUS);
        }
        try {
            ProposalStatus status = ProposalStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
            if (!ProposalStatus.ACCEPTED.equals(status) && !ProposalStatus.REJECTED.equals(status)) {
                throw new CustomException(ErrorCode.INVALID_PROPOSAL_STATUS);
            }
            return status;
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_PROPOSAL_STATUS);
        }
    }

    private void requireUser(String role) {
        if (!ROLE_USER.equals(role)) {
            throw new CustomException(ErrorCode.PROPOSAL_FREELANCER_ONLY);
        }
    }

    private void requireCompany(String role) {
        if (!ROLE_COMPANY.equals(role)) {
            throw new CustomException(ErrorCode.PROPOSAL_COMPANY_ONLY);
        }
    }
}
