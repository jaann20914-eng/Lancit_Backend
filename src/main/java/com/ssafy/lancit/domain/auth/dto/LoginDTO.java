package com.ssafy.lancit.domain.auth.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
 

@Getter @Setter
public class LoginDTO {
    // Request
    @Schema(example = "test@lancit.com")
    private String email;
    @Schema(example = "password")
    private String password;
    @Schema(example = "user", allowableValues = {"user", "company"})
    private String role;

    // Response
    private String accessToken;
    private List<Integer> chatRoomIds; // 로그인 직후 STOMP 구독용
}
