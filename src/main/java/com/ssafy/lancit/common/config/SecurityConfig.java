package com.ssafy.lancit.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ssafy.lancit.common.jwt.JwtAuthenticationFilter;
import com.ssafy.lancit.common.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // csrf 비활성화
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 없이 토큰만 사용
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll() 
                
                
                // TODO 지원: 개발 완료 후 아래로 교체
                //   .requestMatchers("/api/auth/**").permitAll()  // 로그인, 회원가입, 비밀번호 찾기
                //   .requestMatchers("/ws/**").permitAll()        // STOMP 핸드쉐이크
                //   .requestMatchers(HttpMethod.POST, "/api/recruitments").hasRole("company")  // 공고 등록은 회사만
                //   .requestMatchers("/api/portfolios/**").hasRole("user")                     // 포트폴리오는 프리랜서만
                //   .anyRequest().authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider), //요청에서 Authorization: Bearer {token} 헤더 확인, 유효하면 security context에 이메일 +역할을 저장
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();
    }
}
