package com.ssafy.lancit.domain.contract.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.contract.dto.ContractDocumentDTO;

@Mapper
public interface ContractDocumentMapper {

    // 계약서 조회
    ContractDocumentDTO findByContractId(
            Integer contractId
    );

    // 계약서 생성
    int insert(
            ContractDocumentDTO dto
    );

    // 계약서 수정
    int update(
            ContractDocumentDTO dto
    );
    
    // 계약 승인 시간 저장
    int updateConfirmedAt(
            Integer contractId
    );
}