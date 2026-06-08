package com.ssafy.lancit.common.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BusinessNumberValidator {

	private final RestTemplate restTemplate;
	
	@Value("${business.api.key}")
	private String apiKey;
	@Value("${business.api.url}")
	private String apiUrl;
	
    public boolean validate(String businessNumber) {
    	try {
    		//요청 바디 만들기
    		Map<String, Object> body = new HashMap<>();
    	    body.put("b_no", List.of(businessNumber));
        	
        	// 국세청 API 호출
        	String url = apiUrl+"?serviceKey="+apiKey;
        	log.debug("[사업자번호 검증] url: {}", url);
        	HttpHeaders headers = new HttpHeaders();
        	headers.setContentType(MediaType.APPLICATION_JSON);
        	HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        	ResponseEntity<Map> response =restTemplate.postForEntity(url, request, Map.class);
        	log.info("response body = {}", response.getBody());
        	
        	if (!response.getStatusCode().is2xxSuccessful()) {
        		throw new CustomException(ErrorCode.BUSINESS_API_ERROR);//200 응답 못받음 에러
        	}
        	 List<Map> data = (List<Map>) response.getBody().get("data");
             if (data == null || data.isEmpty()) {
                 throw new CustomException(ErrorCode.BUSINESS_API_ERROR);//200 응답 못받음 에러
             }
        	
             String statusCode = (String) data.get(0).get("b_stt_cd");// 값확인
             return "01".equals(statusCode);
        	
    	}catch(CustomException e) {
    		throw e;
    	}catch(Exception e) {
    		log.error("[사업자번호 검증] API 호출 실패 - {}", e.getMessage());
            throw new CustomException(ErrorCode.BUSINESS_API_ERROR);
    	}
    }
	
	
}
