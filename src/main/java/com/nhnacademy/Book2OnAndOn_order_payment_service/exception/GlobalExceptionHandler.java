package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.AmountMismatchException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e){
        return ResponseEntity.ok(toErrorResponse(e));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(DuplicateException e){
        return ResponseEntity.ok(toErrorResponse(e));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e){
        log.warn("Toss Payments API 승인 실패");
        return ResponseEntity.ok(toErrorResponse(e));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException e){
        return ResponseEntity.ok(toErrorResponse(e));
    }

    @ExceptionHandler(AmountMismatchException.class)
    public ResponseEntity<ErrorResponse> handleAmountMismatchException(AmountMismatchException e){
        return ResponseEntity.ok(toErrorResponse(e));
    }

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

    private ErrorResponse toErrorResponse(RuntimeException e){
        if(e instanceof NotFoundException){
            return new ErrorResponse(HttpStatus.NOT_FOUND.name(), e.getMessage(), HttpStatus.NOT_FOUND.value());
        }else if(e instanceof DuplicateException){
            return new ErrorResponse(HttpStatus.CONFLICT.name(), e.getMessage(), HttpStatus.CONFLICT.value());
        }else if(e instanceof FeignException fe){
            String body = fe.contentUTF8();
            int status = fe.status();
            ErrorResponse err = null;
            try {
                err = objectMapper.readValue(body, ErrorResponse.class);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            return new ErrorResponse(err.code(), err.message(), status);
        }else if(e instanceof AmountMismatchException) {
            return new ErrorResponse(HttpStatus.BAD_REQUEST.name(), e.getMessage(), HttpStatus.BAD_REQUEST.value());
        }
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.name(), e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
