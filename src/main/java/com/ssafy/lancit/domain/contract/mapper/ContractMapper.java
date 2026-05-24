package com.ssafy.lancit.domain.contract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.contract.dto.ContractDTO;

@Mapper
public interface ContractMapper {

    // TODO 지원: ContractMapper.xml → SELECT * FROM contract
    //            WHERE (freelancer_email = #{email} OR company_email = #{email})
    //              AND status = #{status} (null 이면 전체)
    //              AND (party_a LIKE #{keyword} OR party_b LIKE #{keyword}) (null 이면 전체)
    //            ORDER BY contract_written_at DESC
    List<ContractDTO> findByUser(@Param("email") String email,
                                 @Param("status") String status,
                                 @Param("keyword") String keyword);

    // TODO 지원: ContractMapper.xml → SELECT * FROM contract WHERE contract_id = #{contractId}
    ContractDTO findById(int contractId);

    // TODO 지원: ContractMapper.xml → INSERT INTO contract (company_email, freelancer_email, status, ...)
    //            useGeneratedKeys="true" keyProperty="contractId" 추가
    //            workDays 는 List<Weekday> → 콤마 구분 문자열로 변환 필요
    //            예) "MON,TUE,WED" 형태로 저장
    void insert(ContractDTO dto);

    // TODO 지원: ContractMapper.xml → UPDATE contract SET status = #{status}, ... WHERE contract_id = #{contractId}
    //            null 인 필드는 업데이트 제외 → <if test="xxx != null"> 사용
    void update(ContractDTO dto);

    // TODO 지원: ContractMapper.xml → SELECT COUNT(*) > 0 FROM contract
    //            WHERE (freelancer_email = #{email} OR company_email = #{email})
    //              AND status IN ('IN_PROGRESS', 'COMPLETED_PENDING')
    boolean hasActiveContract(String email);
}