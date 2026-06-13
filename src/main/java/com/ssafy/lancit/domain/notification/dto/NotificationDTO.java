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
}