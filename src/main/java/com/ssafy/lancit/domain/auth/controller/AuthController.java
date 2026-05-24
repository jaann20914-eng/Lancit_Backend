package com.ssafy.lancit.domain.auth.controller;

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

import lombok.RequiredArgsConstructor;
 
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
 
    private final AuthService authService;
 
    /** AUTH-02(a)(b) 회원가입 - role 로 USER/COMPANY 구분 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupDTO dto) {
        // TODO 지원: authService.signup(dto) 호출 후 201 반환
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** AUTH-04 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginDTO>> login(@RequestBody LoginDTO dto) {
        // TODO 지원: authService.login(dto) → accessToken 담아서 반환
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** USER-01 / CLI-USER-01 로그아웃 (토큰 블랙리스트 처리) */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        // TODO 지원: authService.logout(token) 호출
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** AUTH-05 비밀번호 찾기 */
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody SignupDTO dto) {
        // TODO 지원: authService.resetPassword(dto.getEmail(), dto.getPassword(), dto.getRole()) 호출
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}