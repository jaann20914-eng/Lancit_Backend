package com.ssafy.lancit.domain.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.ssafy.lancit.domain.chat.dto.ChatMessageRequest;
import com.ssafy.lancit.domain.chat.dto.MessageDTO;
import com.ssafy.lancit.domain.chat.service.ChatService;
import com.ssafy.lancit.global.enums.MessageType;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;


    // 텍스트 메시지 전송  
    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessageRequest request, Principal principal) {

        String senderEmail = principal.getName();

        MessageDTO message = chatService.sendMessage(
                request.getChatRoomId(),
                request.getContent(),
                senderEmail,
                MessageType.TEXT
        );

        messagingTemplate.convertAndSend(
                "/sub/chat/" + request.getChatRoomId(),
                message
        );
    }


    // 파일 메시지 전송
    @MessageMapping("/chat/file")
    public void sendFile(ChatMessageRequest request, Principal principal) {
    	String senderEmail = principal.getName();
    	
        MessageDTO message = chatService.sendFileMessage(
                request.getChatRoomId(),
                request.getFileId(),
                senderEmail,
                MessageType.FILE
        );

        messagingTemplate.convertAndSend(
                "/sub/chat/" + request.getChatRoomId(),
                message
        );
    }
}