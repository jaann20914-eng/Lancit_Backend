package com.ssafy.lancit.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.lancit.common.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;
 
@Slf4j
@RestControllerAdvice // 모든 컨트롤러에서 발생하는 예외를 여기서 전부 잡아줌 : @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {
 
    @ExceptionHandler(CustomException.class) // 커스텀 예외 잡는 핸들러 , 형태 : { success: false, message: "리소스를 찾을 수 없습니다." }
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("[CustomException] {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getMessage()));
    }
 
    @ExceptionHandler(MethodArgumentNotValidException.class)  //@Valid 검증 실패 시 (이메일 형식 틀림, 필드 누락 등), 형태 : { success: false, message: "검증 오류 메시지" }
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }
 
    @ExceptionHandler(Exception.class) //fallback 오류
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[Exception] ", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}

// 정상응답 + 오류 응답도 전부 ApiResponse 모양으로감싸서 응답해줌