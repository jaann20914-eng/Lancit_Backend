package com.ssafy.lancit.domain.proposal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.proposal.dto.ProposalDTO;
import com.ssafy.lancit.global.enums.ProposalStatus;

@Mapper
public interface ProposalMapper {
    List<ProposalDTO> findByFreelancer(@Param("email") String email,
                                        @Param("pageRequest") PageRequest pageRequest);
    long countByFreelancer(String email);

    List<ProposalDTO> findByCompany(@Param("email") String email,
                                     @Param("pageRequest") PageRequest pageRequest);
    long countByCompany(String email);

    ProposalDTO findById(int proposalId);
    void insert(ProposalDTO dto);
    void updateStatus(@Param("proposalId") int proposalId, @Param("status") ProposalStatus status);
}