package com.ssafy.lancit.domain.user.dto;

import com.ssafy.lancit.global.enums.JobCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor 
@AllArgsConstructor @Builder
public class UserDTO {
    private String email;               // PK
    private String password;            // BCrypt (수정 요청 시에만 포함)
    private String name;
    private String phone;
    private JobCategory jobCategory;
    private boolean pushable;           // 알림 수신 여부
    private Integer profileFileId;      // FK → file (null 허용)
    private boolean isBookmarked; // DB 컬럼 아님, 조회 시 조립
}