package com.ssafy.lancit.domain.contract.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.contract.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.global.enums.ContractStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 계약서 CRUD + 채팅방 조회
// 서명 이미지 Redis 캐싱은 ContractPdfService 에서 처리
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;
    private final ChatRoomMapper chatRoomMapper;

    // CONT-01 / CLI-CONT-01 계약서 목록 조회 (상태/키워드 필터 + 페이지네이션)
    // role 로 USER(freelancerEmail) / COMPANY(companyEmail) 분기는 XML 에서 처리
    public PageResponse<ContractDTO> getList(String email, String role, String status,
                                              String keyword, PageRequest pageRequest) {
        // TODO 지원 [1]: List<ContractDTO> list = contractMapper.findByUser(email, status, keyword, pageRequest)
        // TODO 지원 [2]: long total = contractMapper.countByUser(email, status, keyword)
        // TODO 지원 [3]: return PageResponse.of(list, total, pageRequest)
        List<ContractDTO> list = contractMapper.findByUser(email, status, keyword, pageRequest);
        long total = contractMapper.countByUser(email, status, keyword);
        return PageResponse.of(list, total, pageRequest);
    }

    // CONT-07 계약서 상세 조회
    public ContractDTO getOne(int contractId) {
        // TODO 지원 [1]: ContractDTO dto = contractMapper.findById(contractId)
        // TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [3]: return dto
        return null;
    }

    // CONT-06 (회사) 계약서 작성 및 전송
    // contractId == 0 → 최초 작성(INSERT, status=NEGOTIATING_A)
    // contractId != 0 → 수정(UPDATE, status NEGOTIATING_A → B → C)
    @Transactional
    public void createOrUpdate(ContractDTO dto, String companyEmail) {
        // TODO 지원 [1]: dto.getContractId() == 0 이면 최초 작성
        //               dto.setCompanyEmail(companyEmail)
        //               dto.setStatus(ContractStatus.NEGOTIATING_A)
        //               contractMapper.insert(dto)
        // TODO 지원 [2]: dto.getContractId() != 0 이면 수정
        //               ContractDTO existing = contractMapper.findById(dto.getContractId())
        //               existing.getCompanyEmail() != companyEmail → throw new CustomException(ErrorCode.FORBIDDEN)
        //               contractMapper.update(dto)
    }

    // CONT-06 (프리랜서) 계약서 수락
    // status → IN_PROGRESS, freelancerSignFileId 업데이트
    @Transactional
    public void accept(int contractId, ContractDTO dto, String freelancerEmail) {
        // TODO 지원 [1]: ContractDTO existing = contractMapper.findById(contractId)
        //               null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [2]: existing.getFreelancerEmail() != freelancerEmail → throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [3]: dto.setContractId(contractId)
        //               dto.setStatus(ContractStatus.IN_PROGRESS)
        //               contractMapper.update(dto)
    }

    // CONT-08 계약 파기 - 프리랜서/회사 양측 모두 가능
    // status → CANCELLED
    @Transactional
    public void cancel(int contractId, String email) {
        // TODO 지원 [1]: ContractDTO existing = contractMapper.findById(contractId)
        //               null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [2]: companyEmail / freelancerEmail 둘 다 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [3]: ContractDTO cancelDto = new ContractDTO()
        //               cancelDto.setContractId(contractId)
        //               cancelDto.setStatus(ContractStatus.CANCELLED)
        //               contractMapper.update(cancelDto)
    }

    // CONT-02 채팅방 조회 - 채팅 페이지 진입 시 chatRoomId 얻기 위해 호출
    public ChatRoomDTO getChatRoom(int contractId) {
        // TODO 지원 [1]: ChatRoomDTO room = chatRoomMapper.findByContract(contractId)
        // TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [3]: return room
        return null;
    }
}