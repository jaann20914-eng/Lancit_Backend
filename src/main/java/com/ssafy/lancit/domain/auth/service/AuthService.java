package com.ssafy.lancit.domain.auth.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.jwt.JwtTokenProvider;
import com.ssafy.lancit.domain.auth.dto.LoginDTO;
import com.ssafy.lancit.domain.auth.dto.SignupDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.contract.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 회원가입 / 로그인 / 비밀번호 변경 / 로그아웃 비즈니스 로직
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final ChatRoomMapper chatRoomMapper;   // 로그인 시 chatRoomIds 조회용
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // AUTH-02 회원가입 - 이메일 중복 확인 → 비밀번호 암호화 → role 분기 INSERT
    @Transactional
    public void signup(SignupDTO dto) {
        // TODO 지원 [1]: role 분기 후(user, company) 이메일 중복 확인
        // TODO 지원 [2]: 중복이면 throw new CustomException(ErrorCode.DUPLICATE_EMAIL)
        // TODO 지원 [3]: 비밀번호 BCrypt 암호화 : dto.setPassword(passwordEncoder.encode(dto.getPassword()))     
        // TODO 지원 [4]: role 분기 후 INSERT
    }

    
    
    //AUTH-04 로그인 :이메일 조회 → 비밀번호 검증 → JWT 발급 → chatRoomIds 조회
    //STOMP: chatRoomIds 포함해줘야 함 → 프론트가 로그인 직후 WebSocket 연결 시
    // 1. /sub/notification/{email} 구독 (알림용)
    // 2. /sub/chat/{chatRoomId} 구독 (진행중인 계약 채팅방 전부)
    public Map<String, Object> login(LoginDTO dto) {
        // TODO 지원 [1]: role 분기 후 이메일로 조회
        // TODO 지원 [2]: 조회 결과 null 이면 throw new CustomException(ErrorCode.INVALID_CREDENTIALS)
        // TODO 지원 [3]: 비밀번호 검증
        //               passwordEncoder.matches(dto.getPassword(), 조회된객체.getPassword())
        //               false 이면 throw new CustomException(ErrorCode.INVALID_CREDENTIALS)
        // TODO 지원 [4]: JWT 발급
        //               String token = jwtTokenProvider.createAccessToken(dto.getEmail(), dto.getRole())
        // TODO 지원 [5]: 진행중인 계약 chatRoomId 목록 조회
        //               List<Integer> chatRoomIds = chatRoomMapper.findIdsByEmail(dto.getEmail())
        // TODO 지원 [6]: Map 으로 묶어서 반환
        //               Map<String, Object> result = new HashMap<>()
        //               result.put("accessToken", token)
        //               result.put("email", dto.getEmail())
        //               result.put("role", dto.getRole())
        //               result.put("chatRoomIds", chatRoomIds)
        //               return result
        return null;
    }

    // AUTH-05 비밀번호 찾기 - 이메일 존재 확인 → 새 비밀번호 암호화 → role 분기 UPDATE
    @Transactional
    public void resetPassword(String email, String newPassword, String role) {
        // TODO 지원 [1]: role 분기 후 이메일 존재 확인
        //               false 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [2]: 새 비밀번호 BCrypt 암호화
        //               String encoded = passwordEncoder.encode(newPassword)
        // TODO 지원 [3]: role 분기 후 UPDATE
        //               "USER"    → userMapper.updatePassword(email, encoded)
        //               "COMPANY" → companyMapper.updatePassword(email, encoded)
    }

    // USER-01 / CLI-USER-01 로그아웃 - A 방식 (클라이언트 토큰 삭제로 처리, 서버는 아무것도 안 함)
    public void logout() {
        // 구현 없음
    }
}