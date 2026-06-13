package com.ssafy.lancit.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

/**
 * byte[] 를 MultipartFile 로 감싸는 구현체
 * MockMultipartFile 은 test 의존성이라 프로덕션에서 사용 불가 → 직접 구현
 */
public class BytesMultipartFile implements MultipartFile {

    private final byte[] bytes;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public BytesMultipartFile(String name, String originalFilename,
                              String contentType, byte[] bytes) {
        this.name             = name;
        this.originalFilename = originalFilename;
        this.contentType      = contentType;
        this.bytes            = bytes != null ? bytes : new byte[0];
    }

    @Override public String getName()             { return name; }
    @Override public String getOriginalFilename() { return originalFilename; }
    @Override public String getContentType()      { return contentType; }
    @Override public boolean isEmpty()            { return bytes.length == 0; }
    @Override public long getSize()               { return bytes.length; }
    @Override public byte[] getBytes()            { return bytes; }
    @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

    @Override
    public void transferTo(java.io.File dest) throws IOException {
        try (var out = new java.io.FileOutputStream(dest)) {
            out.write(bytes);
        }
    }
}