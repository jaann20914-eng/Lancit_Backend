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
    long countByFreelancer(@Param("email") String email);

    List<ProposalDTO> findByCompany(@Param("email") String email,
                                     @Param("pageRequest") PageRequest pageRequest);
    long countByCompany(@Param("email") String email);

    ProposalDTO findById(@Param("proposalId") int proposalId);

    int insert(ProposalDTO dto);

    int updateStatusIfPending(@Param("proposalId") int proposalId,
                              @Param("status") ProposalStatus status);

    int attachContract(@Param("proposalId") int proposalId,
                       @Param("contractId") int contractId);
}
