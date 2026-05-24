package com.ssafy.lancit.domain.contract.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.contract.dto.MessageDTO;
import com.ssafy.lancit.domain.contract.mapper.MessageMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageMapper messageMapper;

    /**
     * CONT-02 채팅방 메시지 목록 조회
     * - is_deleted = true 인 메시지는 XML 에서 제외 처리
     *
     * TODO 지원 [1]: messageMapper.findByChatRoom(chatRoomId) 호출 후 반환
     */
    public List<MessageDTO> getMessages(int chatRoomId) {
        // TODO 지원 [1] 구현
        return null;
    }

    /**
     * CONT-03 메시지 저장
     * - StompChatController.sendMessage() 에서 호출
     *
     * TODO 지원 [1]: messageMapper.insert(dto) 호출
     *               - created_at = NOW() 는 XML 에서 처리
     *               - is_read = false, is_deleted = false, is_updated = false 기본값 세팅
     * TODO 지원 [2]: insert 후 dto 반환 (messageId 포함)
     *               - StompChatController 에서 브로드캐스트 시 사용
     */
    @Transactional
    public MessageDTO save(MessageDTO dto) {
        // TODO 지원 [1] ~ [2] 구현
        return null;
    }

    /**
     * CONT-04 메시지 수정
     * - StompChatController.updateMessage() 에서 호출
     * - 본인 메시지만 수정 가능
     *
     * TODO 지원 [1]: messageMapper.findOwnerEmail(dto.getMessageId()) 로 발신자 확인
     *               - principal.getName() 과 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
     * TODO 지원 [2]: dto.setIsUpdated(true) 세팅
     * TODO 지원 [3]: messageMapper.update(dto) 호출
     * TODO 지원 [4]: 수정된 dto 반환
     */
    @Transactional
    public MessageDTO update(MessageDTO dto) {
        // TODO 지원 [1] ~ [4] 구현
        return null;
    }

    /**
     * CONT-05 메시지 soft delete
     * - StompChatController.deleteMessage() 에서 호출
     * - DB 에서 실제 삭제 안 함 → isDeleted = true 로만 변경
     * - 본인 메시지만 삭제 가능
     *
     * TODO 지원 [1]: messageMapper.findOwnerEmail(dto.getMessageId()) 로 발신자 확인
     *               - principal.getName() 과 다르면 throw new CustomException(ErrorCode.FORBIDDEN)
     * TODO 지원 [2]: dto.setIsDeleted(true) 세팅
     * TODO 지원 [3]: messageMapper.update(dto) 호출
     * TODO 지원 [4]: 삭제 처리된 dto 반환
     *               - 프론트에서 isDeleted=true 수신 시 "삭제된 메시지입니다" 표시
     */
    @Transactional
    public MessageDTO softDelete(MessageDTO dto) {
        // TODO 지원 [1] ~ [4] 구현
        return null;
    }
}