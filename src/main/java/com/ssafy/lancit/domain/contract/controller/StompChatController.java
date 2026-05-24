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
     * 구독자 수신: /sub/chat/{chatRoomId}
     *
     * TODO 지원 [1]: dto.getEmail() 이 null 이면 STOMP 세션에서 발신자 이메일 꺼내기
     *               - 파라미터에 java.security.Principal principal 추가
     *               - principal.getName() → 핸드쉐이크 때 SecurityContext 에 저장된 이메일
     *               - dto.setEmail(principal.getName())
     * TODO 지원 [2]: MessageDTO savedDto = chatService.save(dto) 호출
     * TODO 지원 [3]: messagingTemplate.convertAndSend(
     *                   "/sub/chat/" + dto.getChatRoomId(), savedDto) 로 브로드캐스트
     * TODO 지원 [4]: 채팅방 밖에 있는 상대방에게 알림 발송
     *               - NotificationDTO 만들어서
     *                 messagingTemplate.convertAndSend(
     *                     "/sub/notification/" + 상대방email,
     *                     NotificationDTO(type=CHAT, message=...)) 호출
     *               - 상대방 이메일은 ChatRoomDTO 에서 조회
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageDTO dto) {

        // TODO 지원 [1] ~ [4] 구현
    }

    /**
     * CONT-04 메시지 수정
     * 클라이언트: SEND /pub/chat.update
     * 구독자 수신: /sub/chat/{chatRoomId}
     *
     * TODO 지원 [1]: chatService.update(dto) 호출 → isUpdated = true 로 DB 업데이트
     * TODO 지원 [2]: messagingTemplate.convertAndSend(
     *                   "/sub/chat/" + dto.getChatRoomId(), updatedDto) 로 브로드캐스트
     */
    @MessageMapping("/chat.update")
    public void updateMessage(@Payload MessageDTO dto) {

        // TODO 지원 [1] ~ [2] 구현
    }

    /**
     * CONT-05 메시지 삭제 (soft delete)
     * 클라이언트: SEND /pub/chat.delete
     * 구독자 수신: /sub/chat/{chatRoomId}
     * - DB 에서 실제 삭제 안 함 → isDeleted = true 로만 변경
     * - 프론트에서 isDeleted = true 인 메시지는 "삭제된 메시지입니다" 표시
     *
     * TODO 지원 [1]: chatService.softDelete(dto) 호출 → isDeleted = true 로 DB 업데이트
     * TODO 지원 [2]: messagingTemplate.convertAndSend(
     *                   "/sub/chat/" + dto.getChatRoomId(), deletedDto) 로 브로드캐스트
     */
    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload MessageDTO dto) {

        // TODO 지원 [1] ~ [2] 구현
    }
}