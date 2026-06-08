package com.ssafy.lancit.domain.contract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;

@Mapper
public interface ContractMapper {
    List<ContractDTO> findByUser(@Param("email") String email,
                                  @Param("status") String status,
                                  @Param("keyword") String keyword,
                                  @Param("pageRequest") PageRequest pageRequest);
    long countByUser(@Param("email") String email,
                     @Param("status") String status,
                     @Param("keyword") String keyword);

    ContractDTO findById(int contractId);
    String findOwnerEmailById(int contractId);
    void insert(ContractDTO dto);
    void update(ContractDTO dto);
    
    // ContractGuardAspect 에서 탈퇴 전 진행 중 계약 확인용
    boolean existsActiveContractByEmail(String email);
    
    
}