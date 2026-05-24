package com.ssafy.lancit.domain.contract.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomDTO {
    private int chatRoomId;
    private int contractId;
    private String freelancerEmail;
    private String companyEmail;
}
 
