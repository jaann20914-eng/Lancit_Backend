package com.ssafy.lancit.domain.contract.mapper;


import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.global.enums.ContractStatus;



@Mapper
public interface ContractMapper {
	
	// 계약 생성
	int insert(ContractDTO dto);

	// 동일 공고+프리랜서 간 진행 중 계약 존재 여부
	boolean existsActiveContract(
	        @Param("recruitmentId") Integer recruitmentId,
	        @Param("freelancerEmail") String freelancerEmail
	);
	

    // ParticipantCheckAspect 검증용
    boolean isParticipant(
            @Param("contractId") Integer contractId,
            @Param("email") String email
    );

    // 계약 아이디로 단건 조회
    ContractDTO findById(Integer contractId);

    // 계약 상태 변경
    int updateStatus(
            @Param("contractId") Integer contractId,
            @Param("status") ContractStatus status
    );

    // 계약 목록 조회
    // 상태 필터 + 키워드 검색 + 페이지네이션
    List<Map<String, Object>> searchContracts(
            Map<String, Object> param
    );

    // 계약 목록 총 개수 조회
    long countContracts(
            Map<String, Object> param
    );

    // 계약 상세 페이지 조회
    // 계약 + 공고 + 회사 + 프리랜서 + 채팅방 정보
    Map<String, Object> findContractDetail(
            Integer contractId
    );

    // 계약 파기 요청 존재 여부
    boolean existsCancelRequest(
            Integer contractId
    );

    // 계약 파기 요청 생성
    int insertCancelRequest(
            @Param("contractId") Integer contractId,
            @Param("requesterEmail") String requesterEmail
    );

    // 계약 파기 요청자 조회
    String findCancelRequesterEmail(
            Integer contractId
    );

    // 계약 완전 삭제
    int deleteContract(
            Integer contractId
    );

    // 계약 종료일 경과 대상 조회
    List<Integer> findCompletedPendingTargets();
    
    //진행중인 계약 있는지 확인
    boolean existsActiveContractByEmail(
            String email
    );
    
  //제안목록 카운팅
    long countProposals(
            Map<String, Object> param
    );
    //제안 목록 조회
    List<Map<String, Object>> searchProposals(
            Map<String, Object> param
    );
    
}