package com.nhnacademy.Book2OnAndOn_order_payment_service.cart.advice;

import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.domain.dto.response.ErrorResponse;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartErrorCode;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.nhnacademy.Book2OnAndOn_order_payment_service.cart")
public class GlobalExceptionHandler {

    // 장바구니 도메인 공통 예외 처리
    @ExceptionHandler(CartException.class)
    public ResponseEntity<ErrorResponse> handleCartException(CartException ex) {
        CartErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                errorCode.name(),                 // 에러 코드 (ex. CART_SIZE_EXCEEDED)
                ex.getMessage() != null
                        ? ex.getMessage()            // 세부 메시지 (비즈니스 예외에서 넣은 detail)
                        : errorCode.getMessage()     // CartErrorCode 기본 메시지
        );

        return ResponseEntity.status(status).body(body);
    }

    // Header 누락 실패
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "MISSING_HEADER",
                "필수 요청 헤더가 누락되었습니다. detail=" + ex.getMessage()
        );

        return ResponseEntity.status(status).body(body);
    }

    // Bean Validation (@Valid) 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "VALIDATION_ERROR",
                "요청 값 검증에 실패했습니다. detail=" + detail
        );

        return ResponseEntity.status(status).body(body);
    }

    // @Validated + @RequestParam / @PathVariable 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "CONSTRAINT_VIOLATION",
                "요청 값 검증에 실패했습니다. detail=" + ex.getMessage()
        );

        return ResponseEntity.status(status).body(body);
    }

    // 그 외 감싸지 않은 IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "BAD_REQUEST",
                ex.getMessage()
        );

        return ResponseEntity.status(status).body(body);
    }

    // 나머지 예기치 못한 예외들
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("Unhandled exception", ex);

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                "INTERNAL_ERROR",
                "서버 내부 오류가 발생했습니다. detail=" + ex.getMessage()
        );

        return ResponseEntity.status(status).body(body);
    }
}
