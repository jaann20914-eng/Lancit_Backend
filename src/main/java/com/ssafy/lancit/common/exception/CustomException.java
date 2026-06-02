package com.ssafy.lancit.common.exception;

import lombok.Getter;


// 커스텀 예외 클래스 - ErrorCode 를 담아 throw 하면 GlobalExceptionHandler 가 잡아서 API 응답으로 변환
@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
 
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}