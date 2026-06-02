package com.ssafy.lancit.domain.contract.service;

import com.ssafy.lancit.common.util.GcsSignedUrlUtil;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

// 계약서 HTML → PDF 변환
// ★ 서명 이미지 Base64 → Redis 캐싱 (sign:base64:{fileId}, TTL 1일)
//    최초 1회만 GCS 다운로드, 이후 Redis 에서 즉시 반환
@Service
@RequiredArgsConstructor
public class ContractPdfService {

    private final TemplateEngine templateEngine;
    private final FileMapper fileMapper;
    private final GcsSignedUrlUtil gcsSignedUrlUtil;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SIGN_CACHE_PREFIX = "sign:base64:";

    // ContractDTO → PDF 생성 후 OutputStream 에 직접 write
    public void generatePdf(ContractDTO dto, OutputStream out) throws Exception {
        // TODO 지원 [1]: Thymeleaf Context 생성 후 dto 필드 전부 세팅
        //               Context context = new Context()
        //               context.setVariable("partyA", dto.getPartyA()) ... 나머지 전부
        // TODO 지원 [2]: 서명 이미지 Base64 세팅 (Redis 캐시 사용)
        //               context.setVariable("repSignBase64",    toBase64Cached(dto.getRepresentativeSignFileId()))
        //               context.setVariable("freeSignBase64",   toBase64Cached(dto.getFreelancerSignFileId()))
        //               context.setVariable("confirmSignBase64",toBase64Cached(dto.getConfirmSignFileId()))
        // TODO 지원 [3]: String html = templateEngine.process("contract-template", context)
        // TODO 지원 [4]: ITextRenderer 로 HTML → PDF 변환
        //               ITextRenderer renderer = new ITextRenderer()
        //               renderer.setDocumentFromString(html)
        //               renderer.layout()
        //               renderer.createPDF(out)
    }

    // 서명 이미지 Base64 변환 - Redis 캐싱 1일 TTL
    // null 이면 null 반환 (서명 안 한 경우)
    private String toBase64Cached(Integer fileId) {
        if (fileId == null) return null;

        String key = SIGN_CACHE_PREFIX + fileId;

        // Redis 캐시 조회
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) return cached;

        // 없으면 GCS 에서 다운로드 → Redis 저장
        try {
            FileDTO fileDTO = fileMapper.findById(fileId);
            String signedUrl = gcsSignedUrlUtil.generateForSign(fileDTO.getUploadPath());

            byte[] bytes;
            try (InputStream is = new URL(signedUrl).openStream()) {
                bytes = is.readAllBytes();
            }

            String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            redisTemplate.opsForValue().set(key, base64, 1, TimeUnit.DAYS);
            return base64;

        } catch (Exception e) {
            return null;
        }
    }
}