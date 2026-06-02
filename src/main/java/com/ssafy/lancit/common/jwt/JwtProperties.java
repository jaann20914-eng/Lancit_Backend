package com.ssafy.lancit.common.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
 
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")

// application.properties 의 jwt.* 설정값을 객체로 바인딩 - JwtTokenProvider 에서 주입받아 사용
public class JwtProperties {
    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}
 