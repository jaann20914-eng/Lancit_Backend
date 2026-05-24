package com.ssafy.lancit.domain.contract.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.contract.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.global.enums.ContractStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;
    private final ChatRoomMapper chatRoomMapper;

    /**
     * CONT-01 / CLI-CONT-01 계약서 목록 조회
     *
     * TODO 지원 [1]: contractMapper.findByUser(email, status, keyword) 호출
     *               - XML 에서 role 구분 없이 email 로 조회
     *                 (freelancer_email = #{email} OR company_email = #{email})
     * TODO 지원 [2]: 조회된 List<ContractDTO> 반환
     */
    public List<ContractDTO> getList(String email, String role, String status, String keyword) {
        // TODO 지원 [1] ~ [2] 구현
        return null;
    }

    /**
     * CONT-07 계약서 상세 조회
     *
     * TODO 지원 [1]: contractMapper.findById(contractId) 호출
     * TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [3]: 조회된 ContractDTO 반환
     */
    public ContractDTO getOne(int contractId) {
        // TODO 지원 [1] ~ [3] 구현
        return null;
    }

    /**
     * CONT-06 (회사) 계약서 작성 및 전송
     * - contractId 없으면 최초 작성 → INSERT (status = NEGOTIATING_A)
     * - contractId 있으면 수정 → UPDATE (status NEGOTIATING_A → B → C)
     * - workDays List<Weekday> → 콤마 구분 문자열 변환 후 저장
     *   예) [MON, TUE, WED] → "MON,TUE,WED"
     *
     * TODO 지원 [1]: dto.getContractId() == 0 이면 최초 작성
     *               - dto.setCompanyEmail(companyEmail)
     *               - dto.setStatus(ContractStatus.NEGOTIATING_A)
     *               - workDays 변환 처리
     *               - contractMapper.insert(dto) 호출
     * TODO 지원 [2]: dto.getContractId() != 0 이면 수정
     *               - 기존 계약서 조회 후 companyEmail 본인 확인
     *                 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
     *               - status 흐름 검증
     *                 NEGOTIATING_A → B → C 순서 외 변경 시 예외 처리
     *               - workDays 변환 처리
     *               - contractMapper.update(dto) 호출
     */
    @Transactional
    public void createOrUpdate(ContractDTO dto, String companyEmail) {
        // TODO 지원 [1] ~ [2] 구현
    }

    /**
     * CONT-06 (프리랜서) 계약서 수락
     * - status → IN_PROGRESS 로 변경
     * - 프리랜서 서명 파일 업데이트
     *
     * TODO 지원 [1]: contractMapper.findById(contractId) 로 계약서 조회
     *               - null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [2]: contract.getFreelancerEmail() 과 freelancerEmail 일치 확인
     *               - 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
     * TODO 지원 [3]: dto.setContractId(contractId)
     *               - dto.setStatus(ContractStatus.IN_PROGRESS)
     *               - dto.setFreelancerSignFileId(...) 서명 파일 ID 세팅
     * TODO 지원 [4]: contractMapper.update(dto) 호출
     */
    @Transactional
    public void accept(int contractId, ContractDTO dto, String freelancerEmail) {
        // TODO 지원 [1] ~ [4] 구현
    }

    /**
     * CONT-08 계약 파기
     * - status → CANCELLED 로 변경
     * - 회사 / 프리랜서 양측 모두 파기 가능
     *
     * TODO 지원 [1]: contractMapper.findById(contractId) 로 계약서 조회
     *               - null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [2]: contract.getCompanyEmail() 또는 contract.getFreelancerEmail() 중
     *               하나라도 email 과 일치하는지 확인
     *               - 둘 다 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
     * TODO 지원 [3]: ContractDTO cancelDto 만들어서
     *               - cancelDto.setContractId(contractId)
     *               - cancelDto.setStatus(ContractStatus.CANCELLED)
     *               - contractMapper.update(cancelDto) 호출
     */
    @Transactional
    public void cancel(int contractId, String email) {
        // TODO 지원 [1] ~ [3] 구현
    }

    /**
     * CONT-02 채팅방 조회
     * - 계약서 ID 로 연결된 ChatRoom 반환
     * - 채팅 페이지 진입 시 chatRoomId 얻기 위해 사용
     *
     * TODO 지원 [1]: chatRoomMapper.findByContract(contractId) 호출
     * TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [3]: 조회된 ChatRoomDTO 반환
     */
    public ChatRoomDTO getChatRoom(int contractId) {
        // TODO 지원 [1] ~ [3] 구현
        return null;
    }
}