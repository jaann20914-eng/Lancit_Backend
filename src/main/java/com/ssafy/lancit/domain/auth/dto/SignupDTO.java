package com.ssafy.lancit.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.ssafy.lancit.global.enums.JobCategory;

import lombok.Getter;
import lombok.Setter;
 
@Getter @Setter
public class SignupDTO {
    private String email;
    private String password;
    private String name;
    private String companyName;        // 회사 전용
    private String phone;
    private JobCategory jobCategory;
    private boolean pushable;    
    private String businessNumber;     // 회사 전용
    @Schema(example = "user", allowableValues = {"user", "company"})
    private String role;               // "user" | "company"
}
