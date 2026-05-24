package com.ssafy.lancit.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class GcsConfig {

    // TODO 지원: application.properties 에 gcs.credentials-path=classpath:gcs-key.json 설정
    //            gcs-key.json 파일을 src/main/resources/ 에 배치
    //            gcs-key.json 은 .gitignore 에 반드시 추가 (보안 키 외부 노출 금지)
    @Value("${gcs.credentials-path}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public GcsConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Storage gcsStorage() throws IOException {
        InputStream is = resourceLoader.getResource(credentialsPath).getInputStream();
        GoogleCredentials credentials = GoogleCredentials.fromStream(is);
        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }
}