package com.ssafy.lancit.domain.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
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
        authService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }    
    
    
    
    //로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse< Map<String, Object>>> login(@RequestBody LoginDTO dto) {
    	Map<String, Object> result = authService.login(dto);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
        
    
    // 비밀번호 업데이트
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody SignupDTO dto) {
        authService.resetPassword(dto.getEmail(), dto.getPassword(), dto.getRole());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    
    
    //이메일 인증코드 발송 : Redis 에 "email:verify:{email}" = "123456" TTL 10분 저장 (MailService 에서 처리)
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendCode(@RequestBody Map<String, String> body) {
    	String purpose = body.get("purpose"); 
    	// purpose 검증
        if (!"signup".equals(purpose) && !"pwreset".equals(purpose)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    	
        mailService.sendVerificationCode(body.get("email"), purpose);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
    
    
    //이메일 인증코드 검증 : Redis 에서 저장된 코드 꺼내서 비교 (MailService 에서 처리) ,일치 시 Redis 에서 즉시 삭제 (1회용)
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(@RequestBody Map<String, String> body) {
    	String email   = body.get("email");
        String code    = body.get("code");
        String purpose = body.get("purpose");
        
        // purpose 검증
        if (!"signup".equals(purpose) && !"pwreset".equals(purpose)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        boolean result = mailService.verify(email, code, purpose);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    

	 // 사업자번호 검증 - 국세청 API 호출 후 유효하면 true 반환
	 @PostMapping("/business/verify")
	 public ResponseEntity<ApiResponse<Boolean>> verifyBusinessNumber(
	         @RequestBody Map<String, String> body) {
	     String businessNumber= body.get("businessNumber");
	     boolean isValidBussinessNumber =authService.verifyBusinessNumber(businessNumber);
	     return ResponseEntity.ok(ApiResponse.ok(isValidBussinessNumber));
	 }
}