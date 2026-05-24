package com.ssafy.lancit.domain.contract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.contract.dto.ContractDTO;

@Mapper
public interface ContractMapper {
    List<ContractDTO> findByUser(@Param("email") String email, @Param("status") String status, @Param("keyword") String keyword);
    ContractDTO findById(int contractId);
    void insert(ContractDTO dto);
    void update(ContractDTO dto);
    boolean hasActiveContract(String email);
}