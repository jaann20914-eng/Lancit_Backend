package com.ssafy.lancit.domain.contract.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.contract.dto.ContractCancelRequestDTO;

@Mapper
public interface ContractCancelRequestMapper {

    // 파기 요청 존재 여부
    boolean existsByContractId(
            Integer contractId
    );

    // 파기 요청 생성
    int insert(
            ContractCancelRequestDTO dto
    );

    // 파기 요청 조회
    ContractCancelRequestDTO findByContractId(
            Integer contractId
    );

    // 요청자 이메일 조회
    String findRequesterEmail(
            Integer contractId
    );

    // 삭제
    int delete(
            Integer contractId
    );
}