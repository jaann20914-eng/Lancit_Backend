package com.ssafy.lancit.domain.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.chat.dto.ChatRoomDTO;

@Mapper
public interface ChatRoomMapper {

    // 계약으로 채팅방 조회
    ChatRoomDTO findByContractId(
            Integer contractId
    );

    // 채팅방 생성
    int insert(
            ChatRoomDTO dto
    );

    // 채팅방 단건조회
    ChatRoomDTO findById(
            Integer chatRoomId
    );
    
    //프리랜서의 채팅방 리스트르를 가져옴
    List<Integer> findChatRoomIdsByFreelancerEmail(
            String email
    );

    //회사의 채팅방 리스트를 가져옴
    List<Integer> findChatRoomIdsByCompanyEmail(
            String email
    );
}