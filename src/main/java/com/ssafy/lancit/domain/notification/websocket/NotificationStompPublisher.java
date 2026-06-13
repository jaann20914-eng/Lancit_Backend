package com.ssafy.lancit.domain.notification.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.domain.notification.dto.NotificationDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationStompPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    //특정 사용자에게 실시간 알림 전송
    //구독 : /sub/notification/{email}
    //전송 : convertAndSend("/sub/notification/" + email) -> 다른 사람 이메일 알면 구독 시도 가능한 방식
    
    
    // 클라이언트쪽 구독 경로 : stompClient.subscribe("/user/notification", ...)
    // Spring Security Principal 기반 사용자별 큐 사용 ->서버는 Principal(name) 기준으로 해당 사용자에게만 전달
    
    public void publish(NotificationDTO notification) {
    	messagingTemplate.convertAndSendToUser(
    		    notification.getReceiverEmail(), // Principal.getName()과 일치해야함
    		    "/notification",
    		    notification
    		);
    	 // 클라이언트구독 경로
    	 // stompClient.subscribe("/user/notification", ...)
    }
}