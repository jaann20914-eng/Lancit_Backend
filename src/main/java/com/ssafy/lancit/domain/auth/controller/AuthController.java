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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * AUTH-02(a)(b) 회원가입
     * - dto.getRole() == "USER"    → user 테이블 INSERT
     * - dto.getRole() == "COMPANY" → company 테이블 INSERT
     * - profileFileId = null 로 가입 (사진은 로그인 후 /api/files/upload 에서 별도 업로드)
     *
     * TODO 지원 [1]: authService.signup(dto) 호출
     * TODO 지원 [2]: 성공 시 201 Created 반환
     *               return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null))
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupDTO dto) {
        // TODO 지원 [1] ~ [2] 구현
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    /**
     * AUTH-04 로그인
     * - role 로 USER / COMPANY 테이블 분기 조회
     * - 비밀번호 BCrypt 검증
     * - 성공 시 JWT accessToken 발급
     *
     * TODO 지원 [1]: authService.login(dto) 호출
     *               - 반환된 LoginDTO 에 accessToken 포함되어 있음
     * TODO 지원 [2]: ApiResponse.ok(loginDTO) 반환
     *               - 프론트는 accessToken 받아서 저장 후 즉시 WebSocket 연결 시작
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginDTO>> login(@RequestBody LoginDTO dto) {
        // TODO 지원 [1] ~ [2] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * USER-01 / CLI-USER-01 로그아웃
     * - 현재 프로젝트는 JWT Stateless 방식이라 서버에 토큰 저장 안 함
     * - 클라이언트에서 토큰 삭제하는 것만으로도 로그아웃 처리 가능
     * - 단, 토큰 만료 전 강제 무효화가 필요하다면 블랙리스트 방식 도입 필요
     *
     * TODO 지원 [1]: 팀과 로그아웃 방식 결정
     *               - 방식 A (단순): 서버는 아무것도 안 하고 200 반환
     *                 → 클라이언트가 토큰 삭제하면 끝
     *               - 방식 B (블랙리스트): authService.logout(token) 에서
     *                 토큰을 만료 시간까지 블랙리스트에 저장 (Redis 필요)
     * TODO 지원 [2]: 결정된 방식으로 구현 후 ApiResponse.ok(null) 반환
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        // TODO 지원 [1] ~ [2] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * AUTH-05 비밀번호 찾기
     * - 이메일로 유저/회사 조회 후 비밀번호 변경
     * - 실제 서비스라면 이메일 인증 후 변경해야 하지만
     *   현재 프로젝트는 이메일 인증 없이 직접 변경으로 처리
     *
     * TODO 지원 [1]: authService.resetPassword(dto.getEmail(), dto.getPassword(), dto.getRole()) 호출
     *               - 내부에서 새 비밀번호 BCrypt 암호화 후 UPDATE
     * TODO 지원 [2]: ApiResponse.ok(null) 반환
     */
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody SignupDTO dto) {
        // TODO 지원 [1] ~ [2] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
    
    /** 이메일 인증코드 발송 */
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendCode(@RequestBody Map<String, String> body) {
        // TODO 지원 [1]: mailService.sendVerificationCode(body.get("email")) 호출
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 이메일 인증코드 검증 */
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(@RequestBody Map<String, String> body) {
        // TODO 지원 [1]: mailService.verify(body.get("email"), body.get("code")) 호출
        // TODO 지원 [2]: 결과 boolean ApiResponse.ok() 에 담아 반환
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}