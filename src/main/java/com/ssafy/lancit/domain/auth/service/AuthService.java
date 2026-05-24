package com.ssafy.lancit.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.jwt.JwtTokenProvider;
import com.ssafy.lancit.domain.auth.dto.LoginDTO;
import com.ssafy.lancit.domain.auth.dto.SignupDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
 
@Service
@RequiredArgsConstructor
public class AuthService {
 
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
 
    /** AUTH-02 회원가입
     *  - role=USER → user 테이블 삽입
     *  - role=COMPANY → company 테이블 삽입
     *  - 이메일 중복 체크 */
    @Transactional
    public void signup(SignupDTO dto) {
        // TODO 지원: role 분기 → 이메일 중복 체크 → 비밀번호 BCrypt 암호화 → INSERT
    }
 
    /** AUTH-04 로그인 */
    public LoginDTO login(LoginDTO dto) {
        // TODO 지원: role 분기 → 이메일로 조회 → 비밀번호 검증(passwordEncoder.matches)
        //   → jwtTokenProvider.createAccessToken(email, role) → accessToken 반환
        return null;
    }
 
    /** AUTH-05 비밀번호 찾기 (임시 비밀번호 발급 또는 직접 변경) */
    @Transactional
    public void resetPassword(String email, String newPassword, String role) {
        // TODO 지원: 이메일 존재 확인 → BCrypt 암호화 → UPDATE
    }
 
    /** USER-01 로그아웃 (토큰 블랙리스트 처리 - 필요 시 Redis 활용) */
    public void logout(String token) {
        // TODO 지원: 블랙리스트 저장 or 클라이언트 측 삭제로 처리
    }
}
 