package com.ssafy.lancit.domain.contract.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.contract.dto.ContractDocumentDTO;

@Service
public class ContractPdfService {

    private final TemplateEngine templateEngine;

    public ContractPdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateContractPdf(ContractDocumentDTO dto) {
        try {
            Context context = new Context();
            context.setVariable("contractDto", dto);

            String html = templateEngine.process("freelancer-contract", context);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);

                ClassPathResource fontResource = new ClassPathResource("fonts/NanumGothic.ttf");

                if (fontResource.exists()) {
                    // Supplier는 run() 시점에 호출됨 - 매번 새 스트림을 열어야 함
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
}