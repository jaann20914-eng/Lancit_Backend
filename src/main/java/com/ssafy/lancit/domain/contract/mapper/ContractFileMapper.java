package com.ssafy.lancit.domain.contract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.contract.dto.ContractFileDTO;

@Mapper
public interface ContractFileMapper {

    // 컨펌파일/PDF 등록
    int insert(
            ContractFileDTO dto
    );

    // 계약의 전체 파일 조회
    List<ContractFileDTO> findByContractId(
            Integer contractId
    );

    // 파일 단건 조회
    ContractFileDTO findById(
            Integer contractFileId
    );

    // PDF 조회
    ContractFileDTO findPdfByContractId(
            Integer contractId
    );

    // 컨펌파일 목록 조회
    List<ContractFileDTO> findConfirmFilesByContractId(
            Integer contractId
    );

    // 삭제
    int delete(
            Integer contractFileId
    );
}