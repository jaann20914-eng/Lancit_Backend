package com.ssafy.lancit.common.config;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * GCS Storage 빈 등록
   GoogleCredentials → 일반 인증용
   ServiceAccountCredentials → 서명 기능 포함된 인증용
 
 * TODO 지원: application.properties 에 gcs.credentials-path=classpath:gcs-key.json 설정
 *            gcs-key.json 파일을 src/main/resources/ 에 배치
 *            gcs-key.json 은 .gitignore 에 반드시 추가 (보안 키 외부 노출 금지)
 */
@Configuration
public class GcsConfig {

    @Value("${gcs.credentials-path}")
    private String credentialsPath;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    private final ResourceLoader resourceLoader;

    public GcsConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Storage gcsStorage() throws IOException { //GCS 연결 객체
        InputStream is = resourceLoader.getResource(credentialsPath).getInputStream();

        // ServiceAccountCredentials: Signed URL 서명에 필요
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(is);

        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }

    //GcsService 에서 버킷명 주입받을 수 있도록 빈으로 노출

    @Bean
    public String gcsBucketName() {
        return bucketName;
    }
}