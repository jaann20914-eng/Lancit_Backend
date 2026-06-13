package com.ssafy.lancit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ssafy.lancit.common.jwt.JwtTokenProvider;

@SpringBootTest
public class JwtTokenTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void generateTestToken() {
        String token = jwtTokenProvider.createAccessToken("test4@lancit.com", "USER");
        System.out.println("===== 테스트용 JWT =====");
        System.out.println("Bearer " + token);
        System.out.println("=======================");
    }
    
    @Test
    void password() {
    	org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            System.out.println(encoder.encode("test1234"));
    }
    
}