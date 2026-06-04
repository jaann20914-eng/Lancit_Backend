package com.ssafy.lancit.domain.auth.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;


// 이메일 인증코드 발송 / 검증 - Redis TTL 10분으로 코드 임시 저장
// @Service 주석 해제 시 SendGrid 설정 필요 (application.properties)
 @Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate; 

    @Value("${spring.mail.from}")
    private String fromEmail;

    private static final long CODE_TTL_MINUTES = 10L;
    private static final String CODE_PREFIX = "email:verify:"; // Redis 키: email:verify:{email}

    // 인증코드 발송 - 6자리 랜덤 코드 생성 → 이메일 발송 → Redis 에 10분 TTL 저장
    public void sendVerificationCode(String toEmail) {
        // TODO 지원 [1]: application.properties 에 spring.mail.from=발신자이메일 추가
        // TODO 지원 [2]: spring.mail.password=SG.발급받은SendGridKey 입력
        // TODO 지원 [3]: SendGrid 콘솔에서 fromEmail Verified Sender 확인
        String code = String.valueOf(new Random().nextInt(888_888) + 111_111);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[LANCIT] 이메일 인증번호");
            helper.setText(
                "<p>인증번호: <b>" + code + "</b></p><p>10분 이내에 입력해주세요.</p>",
                true
            );
            mailSender.send(message);

            // Redis 에 10분 TTL 로 저장 → 만료 시 자동 삭제
            redisTemplate.opsForValue().set(CODE_PREFIX + toEmail, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    // 인증코드 검증 - Redis 에서 꺼내서 비교 → 일치 시 즉시 삭제 (1회용)
    public boolean verify(String email, String inputCode) {
        String key = CODE_PREFIX + email;

        // Redis TTL 만료 시 null 반환 → 자동 만료 처리
        String saved = redisTemplate.opsForValue().get(key);
        if (saved == null) return false;

        boolean ok = saved.equals(inputCode);
        if (ok) {
            redisTemplate.delete(key); // 1회 사용 후 즉시 삭제
        }
        return ok;
    }
}