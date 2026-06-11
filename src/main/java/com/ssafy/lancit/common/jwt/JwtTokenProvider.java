package com.ssafy.lancit.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.util.RoleUtil;
 
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
 
@Slf4j
@Component
@RequiredArgsConstructor


//JWT 생성 + 검증 + 파싱 : 로그인 시 토큰 발급, 요청마다 토큰 유효성 검사 및 이메일/역할 추출

public class JwtTokenProvider {
 
    private final JwtProperties jwtProperties;
    
    // secret 키 → 암호화 키 객체로 변환 (서명/검증에 사용)
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
 
    // 로그인 성공 시 JWT 토큰 생성
    public String createAccessToken(String email, String role) {
        String normalizedRole = RoleUtil.normalizeRole(role);
        return Jwts.builder()
                .subject(email) // 토큰 안에 email
                .claim("role", normalizedRole) // role
                .issuedAt(new Date()) // 발급시간
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration())) // 만료시간 을 담고
                .signWith(getKey()) // 암호화 시킴
                .compact();
    }
 
    
    
    // 토큰에서 이메일 꺼내기 함수 (JwtAuthenticationFilter에서 사용)
    public String getEmail(String token) { 
        return getClaims(token).getSubject();
    }
    //토큰에서 역할(user/company) 꺼내기 함수 (JwtAuthenticationFilter에서 사용)
    public String getRole(String token) {
        return RoleUtil.normalizeRole(getClaims(token).get("role", String.class));
    }
    // 토큰 유효한지 검증 (만료/위변조 확인) (JwtAuthenticationFilter에서 사용)
    public boolean validate(String token) {
        try {
            Claims claims = getClaims(token);
            RoleUtil.normalizeRole(claims.get("role", String.class));
            return true;
        } catch (JwtException | IllegalArgumentException | CustomException e) {
            log.warn("[JWT] 유효하지 않은 토큰: {}", e.getMessage());
            return false;
        }
    }
    
    
    //토큰 복호화해서 내부 데이터 꺼내기 -> 해당 클래스에서 내부적으로 사용하기 위한것
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
