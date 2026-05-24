package com.ssafy.lancit.domain.auth.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    // TODO 지원 [1]: application.properties 에 spring.mail.from=발신자이메일 추가
    @Value("${spring.mail.from}")
    private String fromEmail;

    private final Map<String, String> codeStore     = new ConcurrentHashMap<>();
    private final Map<String, Long>   timestampStore = new ConcurrentHashMap<>();
    private static final long CODE_TTL = 10 * 60 * 1000L; // 10분

    /**
     * 인증코드 발송
     * AUTH-02 회원가입 이메일 인증 / AUTH-05 비밀번호 찾기 이메일 인증 공용
     *
     * TODO 지원 [2]: SendGrid API Key application.properties 에 입력
     *               spring.mail.password=SG.발급받은키
     * TODO 지원 [3]: SendGrid 콘솔에서 발신자 이메일 Verified 확인
     *               fromEmail 과 SendGrid Verified Sender 가 일치해야 발송됨
     */
    public void sendVerificationCode(String toEmail) {
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

            codeStore.put(toEmail, code);
            timestampStore.put(toEmail, System.currentTimeMillis());

        } catch (Exception e) {
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    /**
     * 인증코드 검증
     * - 만료(10분) 체크
     * - 일치 시 1회 사용 후 codeStore 에서 제거
     */
    public boolean verify(String email, String inputCode) {
        String saved = codeStore.get(email);
        Long ts      = timestampStore.get(email);

        if (saved == null || ts == null) return false;

        boolean ok = (System.currentTimeMillis() - ts) <= CODE_TTL
                  && saved.equals(inputCode);

        if (ok) {
            codeStore.remove(email);
            timestampStore.remove(email);
        }
        return ok;
    }
}