package com.ssafy.lancit.domain.contract.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.ssafy.lancit.domain.contract.dto.MessageDTO;
import com.ssafy.lancit.domain.contract.service.ChatService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StompChatController {
 
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
 
    /**
     * CONT-03 메시지 전송
     * 클라이언트: SEND /pub/chat.send
     * 서버 → 클라이언트 구독: /sub/chat/{chatRoomId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageDTO dto) {
        // TODO 지원: chatService.save(dto)
        //   저장 후 messagingTemplate.convertAndSend("/sub/chat/" + dto.getChatRoomId(), savedDto)
    }
 
    /**
     * CONT-04 메시지 수정
     * 클라이언트: SEND /pub/chat.update
     */
    @MessageMapping("/chat.update")
    public void updateMessage(@Payload MessageDTO dto) {
        // TODO 지원: chatService.update(dto) → isUpdated=true
        //   messagingTemplate.convertAndSend("/sub/chat/" + dto.getChatRoomId(), updatedDto)
    }
 
    /**
     * CONT-05 메시지 삭제 (soft delete)
     * 클라이언트: SEND /pub/chat.delete
     */
    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload MessageDTO dto) {
        // TODO 지원: chatService.softDelete(dto) → isDeleted=true
        //   messagingTemplate.convertAndSend("/sub/chat/" + dto.getChatRoomId(), deletedDto)
    }
    

}
