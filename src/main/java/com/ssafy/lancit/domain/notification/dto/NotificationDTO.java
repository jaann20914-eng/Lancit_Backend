package com.ssafy.lancit.domain.notification.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDTO {
    private String targetEmail;
    private String message;
    private NotificationType type;
    private LocalDateTime createdAt;
}
