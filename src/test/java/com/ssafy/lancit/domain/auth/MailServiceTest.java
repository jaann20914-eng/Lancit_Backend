package com.ssafy.lancit.domain.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
 
import java.util.concurrent.TimeUnit;
 
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
 
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.auth.service.MailService;
 
import jakarta.mail.internet.MimeMessage;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class MailServiceTest {
 
    @InjectMocks private MailService mailService;
 
    @Mock private JavaMailSender mailSender;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private MimeMessage mimeMessage;
 
    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        ReflectionTestUtils.setField(mailService, "fromEmail", "lancit@test.com");
    }
 
    // ═══════════════════════════════════════════════════════
    //  sendVerificationCode()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("sendVerificationCode() 인증코드 발송")
    class SendCodeTest {
 
        @Test
        @DisplayName("signup 목적 인증코드 발송 - Redis 에 signup:code:{email} 저장")
        void send_signup_success() {
            mailService.sendVerificationCode("user@test.com", "signup");
 
            verify(mailSender, times(1)).send(any(MimeMessage.class));
            verify(valueOps, times(1)).set(
                eq("signup:code:user@test.com"),
                anyString(),
                eq(10L),
                eq(TimeUnit.MINUTES)
            );
        }
 
        @Test
        @DisplayName("pwreset 목적 인증코드 발송 - Redis 에 pwreset:code:{email} 저장")
        void send_pwreset_success() {
            mailService.sendVerificationCode("user@test.com", "pwreset");
 
            verify(mailSender, times(1)).send(any(MimeMessage.class));
            verify(valueOps, times(1)).set(
                eq("pwreset:code:user@test.com"),
                anyString(),
                eq(10L),
                eq(TimeUnit.MINUTES)
            );
        }
 
        @Test
        @DisplayName("메일 발송 실패 시 MAIL_SEND_FAILED 예외")
        void send_mailFailed() {
            doThrow(new RuntimeException("SMTP 오류")).when(mailSender).send(any(MimeMessage.class));
 
            assertThatThrownBy(() -> mailService.sendVerificationCode("user@test.com", "signup"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.MAIL_SEND_FAILED.getMessage());
        }
 
        @Test
        @DisplayName("메일 발송 실패 시 Redis 저장 안 됨")
        void send_mailFailed_noRedis() {
            doThrow(new RuntimeException("SMTP 오류")).when(mailSender).send(any(MimeMessage.class));
 
            try {
                mailService.sendVerificationCode("user@test.com", "signup");
            } catch (CustomException ignored) {}
 
            verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any());
        }
    }
 
    // ═══════════════════════════════════════════════════════
    //  verify()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("verify() 인증코드 검증")
    class VerifyTest {
 
        @Test
        @DisplayName("signup 인증코드 일치 시 true 반환 + verified 키 저장")
        void verify_signup_success() {
            given(valueOps.get("signup:code:user@test.com")).willReturn("123456");
 
            boolean result = mailService.verify("user@test.com", "123456", "signup");
 
            assertThat(result).isTrue();
            verify(redisTemplate, times(1)).delete("signup:code:user@test.com");
            verify(valueOps, times(1)).set(
                eq("signup:verified:user@test.com"),
                eq("true"),
                eq(30L),
                eq(TimeUnit.MINUTES)
            );
        }
 
        @Test
        @DisplayName("pwreset 인증코드 일치 시 true 반환 + pwreset:verified 키 저장")
        void verify_pwreset_success() {
            given(valueOps.get("pwreset:code:user@test.com")).willReturn("654321");
 
            boolean result = mailService.verify("user@test.com", "654321", "pwreset");
 
            assertThat(result).isTrue();
            verify(redisTemplate, times(1)).delete("pwreset:code:user@test.com");
            verify(valueOps, times(1)).set(
                eq("pwreset:verified:user@test.com"),
                eq("true"),
                eq(30L),
                eq(TimeUnit.MINUTES)
            );
        }
 
        @Test
        @DisplayName("인증코드 불일치 시 false 반환")
        void verify_wrongCode() {
            given(valueOps.get("signup:code:user@test.com")).willReturn("123456");
 
            boolean result = mailService.verify("user@test.com", "000000", "signup");
 
            assertThat(result).isFalse();
            verify(redisTemplate, never()).delete(anyString());
            verify(valueOps, never()).set(anyString(), eq("true"), anyLong(), any());
        }
 
        @Test
        @DisplayName("Redis 에 코드 없음 (만료) 시 false 반환")
        void verify_expired() {
            given(valueOps.get("signup:code:user@test.com")).willReturn(null);
 
            boolean result = mailService.verify("user@test.com", "123456", "signup");
 
            assertThat(result).isFalse();
        }
 
        @Test
        @DisplayName("인증코드 1회용 - 검증 후 코드 삭제 확인")
        void verify_oneTimeUse() {
            given(valueOps.get("signup:code:user@test.com")).willReturn("123456");
 
            mailService.verify("user@test.com", "123456", "signup");
 
            verify(redisTemplate, times(1)).delete("signup:code:user@test.com");
        }
 
        @Test
        @DisplayName("signup 코드로 pwreset 검증 불가 - 키 분리 확인")
        void verify_keyIsolation() {
            // signup 코드만 저장
            given(valueOps.get("signup:code:user@test.com")).willReturn("123456");
            given(valueOps.get("pwreset:code:user@test.com")).willReturn(null);
 
            // pwreset 으로 검증 시도 → null 이라 false
            boolean result = mailService.verify("user@test.com", "123456", "pwreset");
 
            assertThat(result).isFalse();
        }
 
        @Test
        @DisplayName("getVerifiedKey 정적 메서드 - 키 형식 확인")
        void getVerifiedKey_format() {
            assertThat(MailService.getVerifiedKey("signup", "user@test.com"))
                    .isEqualTo("signup:verified:user@test.com");
            assertThat(MailService.getVerifiedKey("pwreset", "user@test.com"))
                    .isEqualTo("pwreset:verified:user@test.com");
        }
    }
}