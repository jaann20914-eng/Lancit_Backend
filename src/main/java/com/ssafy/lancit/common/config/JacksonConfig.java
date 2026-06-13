package com.ssafy.lancit.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // ContractService의 convertValue(Map -> ContractDocumentDTO) 전용
    // Jackson3 환경에서는 Jackson2 ObjectMapper가 자동등록되지 않으므로 직접 빈 등록
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}