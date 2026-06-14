package com.ssafy.lancit.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
//WebSocket 라우터 설정 - /pub/** 은 컨트롤러로, /sub/** 은 구독자에게 브로드캐스팅
//일반 rest api : 클라이언트가 요청후 -> 서버가 응답
//스톰프 : 서버가 먼저 클라이언트에 보내줄 수 있음
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.ssafy.lancit.common.websocket.StompAuthInterceptor;

import lombok.RequiredArgsConstructor;

//1. 엔드포인트 연결 /ws : ws://localhost:8080/ws 으로 보내면 일반 http 연결을 websocket으로 업그레이드
//2. 구독 /sub: 클라이언트가 "이 채널 메시지 받을게요" 등록하는 것 (채팅방 입장)
//3. 발행 /pub : 클라이언트가 서버로 메시지 보내는 것 (클라이언트가 서버로 메세지 모내는 것)



@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    // TODO 지원: application.properties 에 stomp.allowed-origins=http://localhost:5173 설정 --> 프론트 포트 CorsConfig 와 동일하게 맞출 것
    @Value("${stomp.allowed-origins}")
    private String allowedOrigins;

    private final StompAuthInterceptor stompAuthInterceptor;
    
    
    //스톰프용 인터셉터 메서드
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
    
    
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.enableSimpleBroker("/sub");
        // 클라이언트 구독 경로
        // 서버가 /sub/** 로 메시지를 발행하면 해당 경로 구독자들에게 전달

        registry.setApplicationDestinationPrefixes("/pub");
        // 클라이언트 → 서버 메시지 전송 경로
        // @MessageMapping 이 처리하는 prefix

        registry.setUserDestinationPrefix("/user");
        // 특정 로그인 사용자 전용 경로
        // 클라이언트 : /user/** 구독
        // 서버 : convertAndSendToUser() 사용

        // 단일 서버에서는 SimpleBroker 사용
        // 추후 서버 확장 시 Redis Pub/Sub 또는 RabbitMQ 등 Message Broker 도입 검토
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }
}

