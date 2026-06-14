package com.ssafy.lancit.domain.chat.dto;

import java.time.LocalDateTime;

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
public class MessageFileDTO {

    private Integer messageFileId;

    private Integer messageId;

    private Integer fileId;

    private LocalDateTime createdAt;
}