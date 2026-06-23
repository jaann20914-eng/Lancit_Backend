package com.ssafy.lancit.domain.contract.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.contract.dto.ContractDocumentDTO;
import com.ssafy.lancit.domain.file.service.FileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractPdfService {

    private final TemplateEngine templateEngine;
    private final FileService fileService;

    public byte[] generateContractPdf(ContractDocumentDTO dto) {
        try {
            Context context = new Context();
            context.setVariable("contractDto", dto);

            // 서명 이미지 signed URL 세팅
            context.setVariable("repSignUrl",      resolveSignedUrl(dto.getRepresentativeSignFileId()));
            context.setVariable("contractSignUrl", resolveSignedUrl(dto.getContractSignFileId()));
            context.setVariable("confirmSignUrl",  resolveSignedUrl(dto.getConfirmSignFileId()));
            context.setVariable("privacySignUrl",  resolveSignedUrl(dto.getPrivacySignFileId()));

            String html = templateEngine.process("freelancer-contract", context);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);

                ClassPathResource fontResource = new ClassPathResource("fonts/NanumGothic.ttf");
                if (fontResource.exists()) {
                    builder.useFont(() -> {
                        try {
                            return fontResource.getInputStream();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, "NanumGothic");
                } else {
                    throw new IllegalStateException("NanumGothic.ttf not found in classpath: fonts/NanumGothic.ttf");
                }

                builder.toStream(baos);
                builder.run();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private String resolveSignedUrl(Integer fileId) {
        if (fileId == null) return null;
        try {
            return fileService.getSignedUrl(fileId);
        } catch (Exception e) {
            return null;
        }
    }
}