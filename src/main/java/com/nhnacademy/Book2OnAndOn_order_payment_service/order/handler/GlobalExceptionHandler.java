package com.nhnacademy.Book2OnAndOn_order_payment_service.order.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Service 계층에서 발생하는 주요 예외를 HTTP 응답 코드로 매핑합니다.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * IllegalArgumentException 발생 시 400 Bad Request를 반환합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        // Validation 오류나 비즈니스 규칙 위반(예: "주문 항목은 반드시 존재해야 합니다.")을 400으로 반환합니다.
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); 
    }
    
    /**
     * AccessDeniedException (권한 없음) 발생 시 403 Forbidden을 반환합니다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {
        return new ResponseEntity<>("접근 권한이 없습니다: " + e.getMessage(), HttpStatus.FORBIDDEN);
    }
    
    // OrderNotFoundException 등 다른 예외 핸들러도 추가 가능
}