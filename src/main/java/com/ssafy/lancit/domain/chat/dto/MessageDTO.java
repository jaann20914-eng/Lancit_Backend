package com.ssafy.lancit.domain.chat.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.MessageType;

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
public class MessageDTO {

    private Integer messageId;

    private Integer chatRoomId;

    private String senderEmail;

    private MessageType messageType;

    private String message;

    private boolean isDeleted;

    private boolean isUpdated;

    private LocalDateTime createdAt;
}