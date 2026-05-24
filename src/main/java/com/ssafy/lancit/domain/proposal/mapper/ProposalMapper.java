package com.ssafy.lancit.domain.proposal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
 
@Mapper
public interface ProposalMapper {
    List<ProposalDTO> findByFreelancer(String email);
    List<ProposalDTO> findByCompany(String email);
    ProposalDTO findById(int proposalId);
    void insert(ProposalDTO dto);
    void updateStatus(@Param("proposalId") int proposalId, @Param("status") String status);
}