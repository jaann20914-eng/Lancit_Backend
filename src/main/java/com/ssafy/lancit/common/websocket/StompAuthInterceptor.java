package com.ssafy.lancit.common.websocket;



import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.common.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
    	
    	//헤더에 접근해서 현재 STOMP 프레임 정보(CONNECT/SEND/DISCONNECT 등) 가져오기
        StompHeaderAccessor accessor =  MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        
        log.info("[STOMP] command={}, sessionId={}, sessionAttrs={}",
                accessor.getCommand(),
                accessor.getSessionId(),
                accessor.getSessionAttributes());

        try {
        	// CONNECT 프레임일 때만 JWT 파싱 (최초 1회)
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                log.info("[STOMP][CONNECT] authHeader={}", authHeader);


                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (jwtTokenProvider.validate(token)) {// 토큰으로 이메일, role 뽑기
                        String email = jwtTokenProvider.getEmail(token);
                        String role = jwtTokenProvider.getRole(token);
                        
                        log.info("[STOMP][CONNECT] email={}, role={}", email, role);
                        
                        if (role != null) {
                            Authentication authentication 
                            = new UsernamePasswordAuthenticationToken( email,
                                            						   null,
                                            						   List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                            //현재(CONNECT 처리) 스레드용 - 아래 로직 1 ) 에서 즉시 사용됨
                            accessor.setUser(authentication);
                            
                            // // STOMP 세션에 저장 -> 이후 SEND 등 다른 스레드에서도 2)로 재사용
                            if (accessor.getSessionAttributes() != null) {
                                accessor.getSessionAttributes().put("AUTH", authentication);
                                log.info("[STOMP][CONNECT] AUTH saved to session. sessionAttrs after put={}",
                                        accessor.getSessionAttributes());
                            }else {
                            	log.warn("[STOMP][CONNECT] sessionAttributes is NULL - cannot save AUTH");
                            }
                        }
                    }else {
                    	log.warn("[STOMP][CONNECT] token validation failed");
                    }
                }else {
                	log.warn("[STOMP][CONNECT] no Authorization header");
                }
            }

            Authentication auth = null;
            // 1) CONNECT 프레임: 방금 위에서 만든 authentication
            if (accessor.getUser() instanceof Authentication a) {
                auth = a;
                log.info("[STOMP][{}] auth from accessor.getUser()", accessor.getCommand());
            // 2) SEND/그 외 프레임: 세션에 저장해둔 인증정보를 꺼냄 (토큰 재파싱 없음)
            } else if (accessor.getSessionAttributes() != null
                    && accessor.getSessionAttributes().get("AUTH") instanceof Authentication a) {
                auth = a;
                log.info("[STOMP][{}] auth from sessionAttributes", accessor.getCommand());
            }else {
                log.warn("[STOMP][{}] no auth found. sessionAttrs={}",
                        accessor.getCommand(), accessor.getSessionAttributes());
            }
            
            
            // 현재 프레임을 처리하는 "이 스레드"의 SecurityContext에 채워줌 : 스레드가 매 프레임마다 바뀌므로 매번 설정해주는 것
            if (auth != null) {
                SecurityContextHolder.getContext().setAuthentication(auth);
                accessor.setUser(auth); 
                log.info("[STOMP][{}] SecurityContext set with principal={}", accessor.getCommand(), auth.getPrincipal());
            } else {
                SecurityContextHolder.clearContext();
            }
            
        } catch (Exception e) {
        	log.error("[STOMP] exception in interceptor", e);
            SecurityContextHolder.clearContext();
        }

        return message;
    }
}