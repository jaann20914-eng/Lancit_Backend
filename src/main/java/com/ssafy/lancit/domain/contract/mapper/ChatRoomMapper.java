package com.ssafy.lancit.domain.contract.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.contract.dto.ChatRoomDTO;

@Mapper
public interface ChatRoomMapper {
    ChatRoomDTO findByContract(int contractId);
    void insert(ChatRoomDTO dto);
}