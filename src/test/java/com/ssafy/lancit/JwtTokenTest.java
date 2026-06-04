package com.ssafy.lancit;

import com.ssafy.lancit.common.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JwtTokenTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void generateTestToken() {
        String token = jwtTokenProvider.createAccessToken("test@lancit.com", "USER");
        System.out.println("===== 테스트용 JWT =====");
        System.out.println("Bearer " + token);
        System.out.println("=======================");
    }
}