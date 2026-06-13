package com.ssafy.lancit.domain.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.chat.dto.MessageDTO;

@Mapper
public interface MessageMapper {

    // 메시지 저장
    int insert(MessageDTO dto);

    // 무한스크롤 조회 (lastMessageId 없으면 최신부터, lastMessageId있다면 거기서 부터 30개 가져옴)
    List<MessageDTO> findMessages(
            @Param("chatRoomId") Integer chatRoomId,
            @Param("lastMessageId") Integer lastMessageId,
            @Param("size") Integer size
    );

    // 단건 조회
    MessageDTO findById(Integer messageId);

    // 소프트 삭제
    int softDelete(Integer messageId);

    // 메시지 수정
    int update(
            @Param("messageId") Integer messageId,
            @Param("content") String content
    );
}