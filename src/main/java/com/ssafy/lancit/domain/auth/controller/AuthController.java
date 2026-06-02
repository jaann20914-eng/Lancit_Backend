package com.ssafy.lancit.domain.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.auth.dto.LoginDTO;
import com.ssafy.lancit.domain.auth.dto.SignupDTO;
import com.ssafy.lancit.domain.auth.service.AuthService;
import com.ssafy.lancit.domain.auth.service.MailService;

import lombok.RequiredArgsConstructor;

// 인증 관련 엔드포인트 (회원가입 / 로그인 / 로그아웃 / 비밀번호 찾기 / 이메일 인증)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MailService mailService;

    
     //AUTH-02(a)(b) 회원가입 : dto.getRole()에 따라 user 테이블 INSERT or company 테이블 INSERT
     //profileFileId = null 로 가입 (사진은 로그인 후 /api/files/upload 에서 별도 업로드)
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupDTO dto) {
        // TODO 지원 [1]: authService.signup(dto) 호출
        // TODO 지원 [2]: 성공 시 201 Created 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }    
//     AUTH-04 로그인 : role 로 USER / COMPANY 테이블 분기 조회 -> 암호화 검증 -> 성공시 토큰 발급
//                      -> chatRoomIds 찾아서 해시맵으로 묶어서 같이 보내주기
//                      -> 프론트가 로그인 직후 WebSocket 연결 하면
//                      1. /sub/notification/{email} 구독 (알림용)
//                      2. /sub/chat/{chatRoomId} 구독 (진행중인 계약 채팅방 전부)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginDTO>> login(@RequestBody LoginDTO dto) {
    	// TODO 지원 [1]: authService.login(dto) 호출
        //               - email + password + role 검증
        //               - JWT accessToken 발급
        //               - 진행중인 계약의 chatRoomId 목록 조회
        // TODO 지원 [2]: Map.of("accessToken", token, "email", email, "role", role, "chatRoomIds", chatRoomIds) 반환
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
    // USER-01 + CLI-USER-01 로그아웃 :  토큰 삭제
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        // TODO 지원 : 토큰삭제
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    
    
    
    // AUTH-05 비밀번호 찾기 : 이메일 인증 완료 후 비밀번호 변경 , 새 비밀번호 BCrypt 암호화는 서비스에서 처리
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody SignupDTO dto) {
        // TODO 지원 [1]: authService.resetPassword(dto.getEmail(), dto.getPassword(), dto.getRole()) 호출
        //               - role 로 user / company 테이블 분기
        //               - 새 비밀번호 BCrypt 암호화 후 UPDATE
        // TODO 지원 [2]: ApiResponse.ok(null) 반환
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    
    
    
    //이메일 인증코드 발송 : Redis 에 "email:verify:{email}" = "123456" TTL 10분 저장 (MailService 에서 처리)
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendCode(@RequestBody Map<String, String> body) {
        // TODO 지원 [1]: mailService.sendVerificationCode(body.get("email")) 호출
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
    //이메일 인증코드 검증 : Redis 에서 저장된 코드 꺼내서 비교 (MailService 에서 처리) ,일치 시 Redis 에서 즉시 삭제 (1회용)
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(@RequestBody Map<String, String> body) {
        // TODO 지원 [1]: boolean result = mailService.verify(body.get("email"), body.get("code")) 호출
        // TODO 지원 [2]: ApiResponse.ok(result) 반환
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}