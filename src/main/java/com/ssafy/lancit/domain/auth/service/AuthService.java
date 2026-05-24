package com.ssafy.lancit.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.jwt.JwtTokenProvider;
import com.ssafy.lancit.domain.auth.dto.LoginDTO;
import com.ssafy.lancit.domain.auth.dto.SignupDTO;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * AUTH-02 회원가입
     *
     * TODO 지원 [1]: dto.getRole() 로 분기
     *               - "USER"    → userMapper.existsByEmail(dto.getEmail())
     *               - "COMPANY" → companyMapper.existsByEmail(dto.getEmail())
     * TODO 지원 [2]: 중복이면 throw new CustomException(ErrorCode.DUPLICATE_EMAIL)
     * TODO 지원 [3]: 비밀번호 BCrypt 암호화
     *               - dto.setPassword(passwordEncoder.encode(dto.getPassword()))
     * TODO 지원 [4]: role 분기 후 INSERT
     *               - "USER"    → UserDTO 만들어서 userMapper.insert(userDto)
     *               - "COMPANY" → CompanyDTO 만들어서 companyMapper.insert(companyDto)
     */
    @Transactional
    public void signup(SignupDTO dto) {
        // TODO 지원 [1] ~ [4] 구현
    }

    /**
     * AUTH-04 로그인
     *
     * TODO 지원 [1]: dto.getRole() 로 분기해서 이메일로 조회
     *               - "USER"    → UserDTO user = userMapper.findByEmail(dto.getEmail())
     *               - "COMPANY" → CompanyDTO company = companyMapper.findByEmail(dto.getEmail())
     * TODO 지원 [2]: 조회 결과 null 이면 throw new CustomException(ErrorCode.INVALID_CREDENTIALS)
     * TODO 지원 [3]: 비밀번호 검증
     *               - passwordEncoder.matches(dto.getPassword(), 조회된객체.getPassword())
     *               - false 이면 throw new CustomException(ErrorCode.INVALID_CREDENTIALS)
     * TODO 지원 [4]: JWT 발급
     *               - String token = jwtTokenProvider.createAccessToken(dto.getEmail(), dto.getRole())
     * TODO 지원 [5]: dto.setAccessToken(token) 세팅 후 반환
     */
    public LoginDTO login(LoginDTO dto) {
        // TODO 지원 [1] ~ [5] 구현
        return null;
    }

    /**
     * AUTH-05 비밀번호 찾기
     * - 이메일 인증 없이 직접 변경 처리
     *
     * TODO 지원 [1]: dto.getRole() 로 분기해서 이메일 존재 확인
     *               - "USER"    → userMapper.existsByEmail(email)
     *               - "COMPANY" → companyMapper.existsByEmail(email)
     *               - false 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [2]: 새 비밀번호 BCrypt 암호화
     *               - String encoded = passwordEncoder.encode(newPassword)
     * TODO 지원 [3]: role 분기 후 UPDATE
     *               - "USER"    → userMapper.updatePassword(email, encoded)
     *               - "COMPANY" → companyMapper.updatePassword(email, encoded)
     */
    @Transactional
    public void resetPassword(String email, String newPassword, String role) {
        // TODO 지원 [1] ~ [3] 구현
    }

    /**
     * USER-01 / CLI-USER-01 로그아웃
     * - JWT Stateless 방식 → 서버는 아무것도 안 함
     * - 클라이언트가 토큰 삭제하면 끝
     * - Redis 미도입으로 블랙리스트 처리 없음
     */
    public void logout(String token) {
        // 구현 없음 - 클라이언트 측 토큰 삭제로 처리
    }
}