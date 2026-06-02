package com.ssafy.lancit.domain.contract.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.contract.dto.MessageDTO;
import com.ssafy.lancit.domain.contract.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 채팅 메시지 저장 / 수정 / soft delete
// 브로드캐스트는 StompChatController 에서 처리
// Redis, 페이지네이션 직접 관련 없음
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageMapper messageMapper;

    // CONT-02 채팅방 메시지 목록 조회 - is_deleted=true 는 XML 에서 제외
    public List<MessageDTO> getMessages(int chatRoomId) {
        // TODO 지원 [1]: return messageMapper.findByChatRoom(chatRoomId)
        return null;
    }

    // CONT-03 메시지 저장 - insert 후 messageId 포함된 dto 반환 (브로드캐스트용)
    @Transactional
    public MessageDTO save(MessageDTO dto) {
        // TODO 지원 [1]: messageMapper.insert(dto)
        //               created_at=NOW(), is_read=false, is_deleted=false, is_updated=false 는 XML 기본값
        // TODO 지원 [2]: return dto (messageId 포함)
        return null;
    }

    // CONT-04 메시지 수정 - 본인 메시지만 수정 가능
    // ★ Principal 은 서비스 접근 불가 → 컨트롤러에서 email 파라미터로 전달
    @Transactional
    public MessageDTO update(MessageDTO dto, String email) {
        // TODO 지원 [1]: String ownerEmail = messageMapper.findOwnerEmail(dto.getMessageId())
        //               ownerEmail 과 email 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [2]: dto.setUpdated(true)
        // TODO 지원 [3]: messageMapper.update(dto)
        // TODO 지원 [4]: return dto
        return null;
    }

    // CONT-05 메시지 soft delete - 본인 메시지만 삭제 가능
    // DB 실제 삭제 안 함 → isDeleted=true 로만 변경
    // 프론트에서 isDeleted=true 수신 시 "삭제된 메시지입니다" 표시
    // ★ Principal 은 서비스 접근 불가 → 컨트롤러에서 email 파라미터로 전달
    @Transactional
    public MessageDTO softDelete(MessageDTO dto, String email) {
        // TODO 지원 [1]: String ownerEmail = messageMapper.findOwnerEmail(dto.getMessageId())
        //               ownerEmail 과 email 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [2]: dto.setDeleted(true)
        // TODO 지원 [3]: messageMapper.update(dto)
        // TODO 지원 [4]: return dto
        return null;
    }
}