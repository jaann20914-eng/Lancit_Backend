package com.ssafy.lancit.domain.auth.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
 

@Getter @Setter
public class LoginDTO {
    // Request
    private String email;
    private String password;
    private String role;

    // Response
    private String accessToken;
    private List<Integer> chatRoomIds; // 로그인 직후 STOMP 구독용
}