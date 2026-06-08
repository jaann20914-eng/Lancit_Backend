package com.ssafy.lancit.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BusinessNumberValidatorTest {
	@Autowired
	BusinessNumberValidator businessNumberValidator;
	
	@Test
    @DisplayName("유효한 사업자번호 검증")
    void validBusinessNumber() {
        boolean result = businessNumberValidator.validate("6888801923");
        System.out.println("[유효한 사업자번호 검증 결과] " + result);
        assertThat(result).isTrue();
    }
 
    @Test
    @DisplayName("유효하지 않은 사업자번호 검증")
    void invalidBusinessNumber() {
        boolean result = businessNumberValidator.validate("0000000000");
        System.out.println("[유효하지 않은 사업자번호 검증 결과] " + result);
        assertThat(result).isFalse();
    }
	
}
