package com.ssafy.lancit.domain.contract.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.contract.dto.ChatRoomDTO;

@Mapper
public interface ChatRoomMapper {

    // TODO 지원: ChatRoomMapper.xml → SELECT * FROM chat_room WHERE contract_id = #{contractId}
    ChatRoomDTO findByContract(int contractId);

    // TODO 지원: ChatRoomMapper.xml → INSERT INTO chat_room (contract_id, freelancer_email, company_email)
    //            useGeneratedKeys="true" keyProperty="chatRoomId" 추가
    void insert(ChatRoomDTO dto);
    
	 // email로 채팅방들 리스트 가져오기
	 List<Integer> findChatRoomIdsByEmail(String email);
}