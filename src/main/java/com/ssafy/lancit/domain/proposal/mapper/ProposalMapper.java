package com.ssafy.lancit.domain.proposal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.global.enums.ProposalStatus;

@Mapper
public interface ProposalMapper {

    // TODO 지원: ProposalMapper.xml → SELECT * FROM proposal
    //            WHERE freelancer_email = #{email}
    //            ORDER BY sent_at DESC
    List<ProposalDTO> findByFreelancer(String email);

    // TODO 지원: ProposalMapper.xml → SELECT * FROM proposal
    //            WHERE company_email = #{email}
    //            ORDER BY sent_at DESC
    List<ProposalDTO> findByCompany(String email);

    // TODO 지원: ProposalMapper.xml → SELECT * FROM proposal
    //            WHERE proposal_id = #{proposalId}
    ProposalDTO findById(int proposalId);

    // TODO 지원: ProposalMapper.xml → INSERT INTO proposal
    //            (company_email, freelancer_email, title, content, status, sent_at)
    //            useGeneratedKeys="true" keyProperty="proposalId" 추가
    void insert(ProposalDTO dto);

    // TODO 지원: ProposalMapper.xml → UPDATE proposal SET status = #{status}
    //            WHERE proposal_id = #{proposalId}
    void updateStatus(@Param("proposalId") int proposalId, @Param("status") ProposalStatus status);
}