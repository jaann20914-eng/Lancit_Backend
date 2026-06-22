package com.ssafy.lancit.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import tools.jackson.databind.SerializationFeature;

@Configuration
public class JacksonConfig {

    // ContractService의 convertValue(Map -> ContractDocumentDTO) 전용
    // Jackson3 환경에서는 Jackson2 ObjectMapper가 자동등록되지 않으므로 직접 빈 등록
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        //mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

