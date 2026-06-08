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

    private static final long CODE_TTL_MINUTES     = 10L;
    private static final long VERIFIED_TTL_MINUTES = 30L;
    // Redis 키 상수
    private static final String CODE_KEY     = "%s:code:%s";     // {purpose}:code:{email}
    private static final String VERIFIED_KEY = "%s:verified:%s"; // {purpose}:verified:{email}

    
    
    // 인증코드 발송 - 6자리 랜덤 코드 생성 → 이메일 발송 → Redis 에 10분 TTL 저장
    // purpose: "signup" || "pwreset"
    public void sendVerificationCode(String toEmail, String purpose) {
        //SendGrid 콘솔에서 fromEmail Verified Sender 확인
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
            String key = String.format(CODE_KEY, purpose, toEmail);
            redisTemplate.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    
    // 인증코드 검증 - Redis 에서 꺼내서 비교 → 일치 시 즉시 삭제 (1회용)
    //signup:verified:email@naver.com -- 회원가입 이메일 인증코드
    //pwreset:verified:email@naver.com -- 비밀번호 업데이트용 이메일 인증코드
    public boolean verify(String email, String inputCode, String purpose) {
        //Redis에서 키 꺼내오기
    	String codeKey= String.format(CODE_KEY,purpose,email);
        String saved=redisTemplate.opsForValue().get(codeKey);

        if (saved == null) return false;

        boolean ok = saved.equals(inputCode);
        if (ok) {
            redisTemplate.delete(codeKey); // 인증코드 삭제
            String verifiedKey = String.format(VERIFIED_KEY, purpose,email);
            redisTemplate.opsForValue().set( verifiedKey, "true", 30, TimeUnit.MINUTES); // 인증 완료 표시 30분
        }
        return ok;
    }
    
    public static String getVerifiedKey(String purpose, String email) {
        return String.format(VERIFIED_KEY, purpose, email);
    }
}