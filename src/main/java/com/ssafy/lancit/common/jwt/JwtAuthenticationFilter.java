package com.ssafy.lancit.common.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
 

// 모든 요청마다 JWT 토큰 검증 후 유효하면 SecurityContext 에 인증 정보 저장 , 필터 등록


@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 
    private final JwtTokenProvider jwtTokenProvider;
 
    
    //모든 요청마다 실행
    //토큰 유효성 검사후 securityContext에 저장
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);// 요청헤더에서 토큰 꺼내기
        
        
        if (token != null && jwtTokenProvider.validate(token)) { // 토큰 유효하면
            String email = jwtTokenProvider.getEmail(token); //이메일, role 뽑아내서
            String role  = jwtTokenProvider.getRole(token);
            var auth = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth); //SecurityContext라는 풀에 저장해놓기 (이 필터를 지난 후에 이후 컨트롤러~서비스~등등에서 사용 가능하도록)
        }
        filterChain.doFilter(request, response);
    }
 
    // 요청 헤더에서 토큰 꺼내기
    private String resolveToken(HttpServletRequest request) {
        //headers: {   프론트 요청 모양
        //    Authorization: `Bearer ${token}`
        //}
    	
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
 