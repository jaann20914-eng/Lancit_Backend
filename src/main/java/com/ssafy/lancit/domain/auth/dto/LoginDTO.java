package com.ssafy.lancit.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;
 
@Getter @Setter
public class LoginDTO {
    // Request
    private String email;
    private String password;
    private String role; // "USER" | "COMPANY"
 
    // Response
    private String accessToken;
}
