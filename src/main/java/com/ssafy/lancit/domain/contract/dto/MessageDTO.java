package com.ssafy.lancit.domain.contract.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageDTO {
    private int messageId;
    private int chatRoomId;
    private String email;           // 발신자 이메일
    private String message;
    private MessageType messageType;
    private boolean isRead;
    private boolean isDeleted;      // soft delete
    private boolean isUpdated;
    private LocalDateTime createdAt;
}