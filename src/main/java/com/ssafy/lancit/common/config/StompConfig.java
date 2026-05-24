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

    // TODO 지원: application.properties 에 stomp.allowed-origins=http://localhost:3000 설정
    //            프론트 포트 CorsConfig 와 동일하게 맞출 것
    @Value("${stomp.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");            // 구독 prefix (클라이언트 수신)
        registry.setApplicationDestinationPrefixes("/pub"); // 발행 prefix (클라이언트 송신)
        // TODO 지원: 추후 서버 스케일 아웃 시
        //            enableSimpleBroker → enableStompBrokerRelay 로 교체 (Redis Pub/Sub)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }
}