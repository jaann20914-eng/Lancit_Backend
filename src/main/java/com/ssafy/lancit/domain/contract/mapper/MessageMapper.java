package com.ssafy.lancit.domain.contract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.contract.dto.MessageDTO;

@Mapper
public interface MessageMapper {

    // TODO 지원: MessageMapper.xml → SELECT * FROM message
    //            WHERE chat_room_id = #{chatRoomId}
    //              AND is_deleted = false
    //            ORDER BY created_at ASC
    List<MessageDTO> findByChatRoom(int chatRoomId);

    // TODO 지원: MessageMapper.xml → INSERT INTO message
    //            (chat_room_id, email, message, message_type, is_read, is_deleted, is_updated, created_at)
    //            useGeneratedKeys="true" keyProperty="messageId" 추가
    void insert(MessageDTO dto);

    // TODO 지원: MessageMapper.xml → UPDATE message SET
    //            message = #{message},       (수정 시)
    //            is_updated = #{isUpdated},
    //            is_deleted = #{isDeleted}   (soft delete 시)
    //            WHERE message_id = #{messageId}
    //            null 인 필드는 업데이트 제외 → <if test="xxx != null"> 사용
    void update(MessageDTO dto);

    // TODO 지원: MessageMapper.xml → SELECT email FROM message WHERE message_id = #{messageId}
    //            OwnerCheckAspect 에서 메시지 발신자 검증 시 사용
    String findOwnerEmail(int messageId);
}