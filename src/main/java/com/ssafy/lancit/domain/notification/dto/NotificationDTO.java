package com.ssafy.lancit.domain.notification.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    private Integer notificationId;

    private String receiverEmail;

    private NotificationType type;

    private Integer targetId;

    private boolean isRead;

    private LocalDateTime createdAt;
    
    
    // 캘린더 납기일 알림 등 별도 메시지가 필요한 경우 사용
    // DB에 저장하지 않고 STOMP 실시간 전송 시에만 사용
    private String message;
}