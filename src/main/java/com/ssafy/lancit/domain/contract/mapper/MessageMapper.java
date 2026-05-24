package com.ssafy.lancit.domain.contract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.contract.dto.MessageDTO;

@Mapper
public interface MessageMapper {
    List<MessageDTO> findByChatRoom(int chatRoomId);
    void insert(MessageDTO dto);
    void update(MessageDTO dto);
    String findOwnerEmail(int messageId);
}
