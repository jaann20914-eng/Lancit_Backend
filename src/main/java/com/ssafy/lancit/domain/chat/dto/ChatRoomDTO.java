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
public class ChatRoomDTO {

    private Integer chatRoomId;

    private Integer contractId;

    private String companyEmail;
    
    private String freelancerEmail;
    
    private LocalDateTime createdAt;
}