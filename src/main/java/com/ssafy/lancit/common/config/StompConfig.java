package com.ssafy.lancit.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
 
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {
 
    @Value("${stomp.allowed-origins}")
    private String allowedOrigins;
 
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");     // 구독 prefix (클라이언트 수신)
        registry.setApplicationDestinationPrefixes("/pub"); // 발행 prefix (클라이언트 송신)
    }
 
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")              // WebSocket 연결 엔드포인트
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }
}
