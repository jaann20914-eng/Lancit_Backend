package com.ssafy.lancit.domain.proposal;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.proposal.dto.ProposalCreateRequest;
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.domain.proposal.dto.ProposalStatusUpdateRequest;
import com.ssafy.lancit.domain.proposal.mapper.ProposalMapper;
import com.ssafy.lancit.domain.proposal.service.ProposalService;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.ProposalStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    private static final String COMPANY_EMAIL = "company@test.com";
    private static final String OTHER_COMPANY_EMAIL = "other-company@test.com";
    private static final String USER_EMAIL = "user@test.com";
    private static final String OTHER_USER_EMAIL = "other-user@test.com";

    @InjectMocks
    private ProposalService proposalService;

    @Mock
    private ProposalMapper proposalMapper;

    @Mock
    private ContractMapper contractMapper;

    @Mock
    private RecruitmentMapper recruitmentMapper;

    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("회사는 자기 공고로 프리랜서에게 PENDING 제안을 보낼 수 있다")
    void send_company_success() {
        ProposalCreateRequest request = createRequest();
        ProposalDTO created = proposal(ProposalStatus.PENDING, null);
        given(recruitmentMapper.findById(10)).willReturn(recruitment());
        given(userMapper.findByEmail(USER_EMAIL)).willReturn(UserDTO.builder().email(USER_EMAIL).build());
        doAnswer(invocation -> {
            ProposalDTO proposal = invocation.getArgument(0);
            proposal.setProposalId(1);
            return 1;
        }).when(proposalMapper).insert(any(ProposalDTO.class));
        given(proposalMapper.findById(1)).willReturn(created);

        ProposalDTO result = proposalService.send(request, COMPANY_EMAIL, "COMPANY");

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.PENDING);
        verify(proposalMapper).insert(any(ProposalDTO.class));
    }

    @Test
    @DisplayName("프리랜서는 제안을 보낼 수 없다")
    void send_userRole_fail() {
        assertCustomException(
                () -> proposalService.send(createRequest(), USER_EMAIL, "USER"),
                ErrorCode.PROPOSAL_COMPANY_ONLY);
        verify(proposalMapper, never()).insert(any());
    }

    @Test
    @DisplayName("받은 프리랜서와 보낸 회사만 제안 상세를 조회할 수 있다")
    void getOne_participants_success() {
        given(proposalMapper.findById(1)).willReturn(proposal(ProposalStatus.PENDING, null));

        assertThat(proposalService.getOne(1, USER_EMAIL, "USER").getProposalId()).isEqualTo(1);
        assertThat(proposalService.getOne(1, COMPANY_EMAIL, "COMPANY").getProposalId()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 프리랜서나 회사는 제안 상세를 조회할 수 없다")
    void getOne_nonParticipant_fail() {
        given(proposalMapper.findById(1)).willReturn(proposal(ProposalStatus.PENDING, null));

        assertCustomException(() -> proposalService.getOne(1, OTHER_USER_EMAIL, "USER"),
                ErrorCode.PROPOSAL_FORBIDDEN);
        assertCustomException(() -> proposalService.getOne(1, OTHER_COMPANY_EMAIL, "COMPANY"),
                ErrorCode.PROPOSAL_FORBIDDEN);
    }

    @Test
    @DisplayName("프리랜서는 받은 제안 목록만 조회할 수 있다")
    void getReceived_user_success() {
        PageRequest pageRequest = new PageRequest();
        given(proposalMapper.findByFreelancer(USER_EMAIL, pageRequest))
                .willReturn(List.of(proposal(ProposalStatus.PENDING, null)));
        given(proposalMapper.countByFreelancer(USER_EMAIL)).willReturn(1L);

        assertThat(proposalService.getReceived(USER_EMAIL, "USER", pageRequest).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("회사는 보낸 제안 목록만 조회할 수 있다")
    void getSent_company_success() {
        PageRequest pageRequest = new PageRequest();
        given(proposalMapper.findByCompany(COMPANY_EMAIL, pageRequest))
                .willReturn(List.of(proposal(ProposalStatus.PENDING, null)));
        given(proposalMapper.countByCompany(COMPANY_EMAIL)).willReturn(1L);

        assertThat(proposalService.getSent(COMPANY_EMAIL, "COMPANY", pageRequest).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("PENDING 제안 수락 시 WAITING 계약을 생성하고 연결한다")
    void accept_pending_success() {
        ProposalDTO pending = proposal(ProposalStatus.PENDING, null);
        ProposalDTO accepted = proposal(ProposalStatus.ACCEPTED, 7);
        given(proposalMapper.findById(1)).willReturn(pending, accepted);
        given(contractMapper.existsActiveContract(10, USER_EMAIL)).willReturn(false);
        given(proposalMapper.updateStatusIfPending(1, ProposalStatus.ACCEPTED)).willReturn(1);
        doAnswer(invocation -> {
            ContractDTO contract = invocation.getArgument(0);
            contract.setContractId(7);
            return 1;
        }).when(contractMapper).insert(any(ContractDTO.class));
        given(proposalMapper.attachContract(1, 7)).willReturn(1);

        ProposalDTO result = proposalService.updateStatus(1, statusRequest("ACCEPTED"), USER_EMAIL, "USER");

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        assertThat(result.getContractId()).isEqualTo(7);
        verify(contractMapper).insert(any(ContractDTO.class));
        verify(proposalMapper).attachContract(1, 7);
    }

    @Test
    @DisplayName("PENDING 제안 거절 시 계약을 생성하지 않는다")
    void reject_pending_success() {
        given(proposalMapper.findById(1)).willReturn(
                proposal(ProposalStatus.PENDING, null), proposal(ProposalStatus.REJECTED, null));
        given(proposalMapper.updateStatusIfPending(1, ProposalStatus.REJECTED)).willReturn(1);

        ProposalDTO result = proposalService.updateStatus(1, statusRequest("REJECTED"), USER_EMAIL, "USER");

        assertThat(result.getStatus()).isEqualTo(ProposalStatus.REJECTED);
        verify(contractMapper, never()).insert(any());
    }

    @Test
    @DisplayName("이미 수락 또는 거절한 제안은 재변경할 수 없어 계약도 중복 생성되지 않는다")
    void updateStatus_alreadyProcessed_fail() {
        given(proposalMapper.findById(1)).willReturn(proposal(ProposalStatus.ACCEPTED, 7));

        assertCustomException(
                () -> proposalService.updateStatus(1, statusRequest("REJECTED"), USER_EMAIL, "USER"),
                ErrorCode.PROPOSAL_STATUS_CONFLICT);

        verify(proposalMapper, never()).updateStatusIfPending(1, ProposalStatus.REJECTED);
        verify(contractMapper, never()).insert(any());
    }

    private ProposalCreateRequest createRequest() {
        ProposalCreateRequest request = new ProposalCreateRequest();
        request.setFreelancerEmail(USER_EMAIL);
        request.setRecruitmentId(10);
        return request;
    }

    private ProposalStatusUpdateRequest statusRequest(String status) {
        ProposalStatusUpdateRequest request = new ProposalStatusUpdateRequest();
        request.setStatus(status);
        return request;
    }

    private RecruitmentDTO recruitment() {
        return RecruitmentDTO.builder()
                .recruitmentId(10)
                .companyEmail(COMPANY_EMAIL)
                .title("백엔드 프로젝트")
                .build();
    }

    private ProposalDTO proposal(ProposalStatus status, Integer contractId) {
        return ProposalDTO.builder()
                .proposalId(1)
                .companyEmail(COMPANY_EMAIL)
                .freelancerEmail(USER_EMAIL)
                .recruitmentId(10)
                .status(status)
                .contractId(contractId)
                .build();
    }

    private void assertCustomException(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }
}
