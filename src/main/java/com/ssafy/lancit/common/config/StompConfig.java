package com.ssafy.lancit.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


//WebSocket 라우터 설정 - /pub/** 은 컨트롤러로, /sub/** 은 구독자에게 브로드캐스
//일반 rest api : 클라이언트가 요청후 -> 서버가 응답
//스톰프 : 서버가 먼저 클라이엉ㄴ트에 보내줄 수 있음

//1. 엔드포인트 연결 /ws : ws://localhost:8080/ws 으로 보내면 일반 http 연결을 websocket으로 업그레이드
//2. 구독 /sub: 클라이언트가 "이 채널 메시지 받을게요" 등록하는 것 (채팅방 입장)
//3. 발행 /pub : 클라이언트가 서버로 메시지 보내는 것 (클라이언트가 서버로 메세지 모내는 것)



@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    // TODO 지원: application.properties 에 stomp.allowed-origins=http://localhost:5173 설정 --> 프론트 포트 CorsConfig 와 동일하게 맞출 것
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

