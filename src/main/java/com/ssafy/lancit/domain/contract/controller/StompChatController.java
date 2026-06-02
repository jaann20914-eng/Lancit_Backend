package com.ssafy.lancit.domain.contract.controller;

import com.ssafy.lancit.domain.contract.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.contract.dto.MessageDTO;
import com.ssafy.lancit.domain.contract.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.contract.service.ChatService;
import com.ssafy.lancit.domain.notification.dto.NotificationDTO;
import com.ssafy.lancit.global.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

// STOMP 채팅 컨트롤러 - /pub/chat.* 로 들어오는 메시지 처리
// REST API 아님 → ApiResponse 사용 안 함, 응답은 STOMP /sub/chat/{chatRoomId} 로 브로드캐스트
@Controller
@RequiredArgsConstructor
public class StompChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ChatRoomMapper chatRoomMapper; // 상대방 이메일 조회용

    // CONT-03 메시지 전송 - DB 저장 후 채팅방 구독자 전체에게 브로드캐스트
    // ★ Principal 로 발신자 이메일 꺼내기 (JWT 필터에서 SecurityContext 에 저장된 값)
    // ★ 채팅방 밖 상대방에게 /sub/notification/{email} 으로 알림 푸시
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageDTO dto, Principal principal) {
        // TODO 지원 [1]: dto.setEmail(principal.getName())
        // TODO 지원 [2]: MessageDTO savedDto = chatService.save(dto)
        // TODO 지원 [3]: messagingTemplate.convertAndSend("/sub/chat/" + dto.getChatRoomId(), savedDto)
        // TODO 지원 [4]: 상대방 알림 발송
        //               ChatRoomDTO room = chatRoomMapper.findByChatRoomId(dto.getChatRoomId())
        //               String otherEmail = dto.getEmail().equals(room.getFreelancerEmail())
        //                   ? room.getCompanyEmail() : room.getFreelancerEmail()
        //               messagingTemplate.convertAndSend("/sub/notification/" + otherEmail,
        //                   NotificationDTO(type=CHAT, message="새 메시지가 도착했습니다."))
    }

    // CONT-04 메시지 수정 - isUpdated=true 로 DB 업데이트 후 브로드캐스트
    @MessageMapping("/chat.update")
    public void updateMessage(@Payload MessageDTO dto, Principal principal) {
        // TODO 지원 [1]: MessageDTO updatedDto = chatService.update(dto, principal.getName());
        //               (ChatService 내부에서 principal.getName() 으로 본인 검증)
        // TODO 지원 [2]: messagingTemplate.convertAndSend("/sub/chat/" + dto.getChatRoomId(), updatedDto)
    }

    // CONT-05 메시지 soft delete - isDeleted=true 로 DB 업데이트 후 브로드캐스트
    // 프론트에서 isDeleted=true 수신 시 "삭제된 메시지입니다" 표시
    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload MessageDTO dto, Principal principal) {
        // TODO 지원 [1]: MessageDTO deletedDto = chatService.softDelete(dto, principal.getName());
        //               (ChatService 내부에서 principal.getName() 으로 본인 검증)
        // TODO 지원 [2]: messagingTemplate.convertAndSend("/sub/chat/" + dto.getChatRoomId(), deletedDto)
    }
}