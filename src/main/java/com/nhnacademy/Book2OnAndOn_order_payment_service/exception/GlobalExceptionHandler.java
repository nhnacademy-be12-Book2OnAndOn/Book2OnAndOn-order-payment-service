package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartErrorCode;
import com.nhnacademy.Book2OnAndOn_order_payment_service.cart.exception.CartException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception.AmountMismatchException;
import feign.FeignException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 공통 ErrorResponse 생성 헬퍼 */
    private ErrorResponse buildErrorResponse(HttpStatus status, String error, String message) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message
        );
    }

    // ==========================
    // 주문/결제
    // ==========================

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, status.name(), e.getMessage()));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(DuplicateException e) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, status.name(), e.getMessage()));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, status.name(), e.getMessage()));
    }

    @ExceptionHandler(AmountMismatchException.class)
    public ResponseEntity<ErrorResponse> handleAmountMismatchException(AmountMismatchException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, status.name(), e.getMessage()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        int rawStatus = e.status();
        HttpStatus status;
        try {
            status = HttpStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException ex) {
            status = HttpStatus.BAD_GATEWAY;   // 외부 연동 에러는 502 정도로 해석
        }

        String body = e.contentUTF8();
        String message = "외부 API 호출 중 오류가 발생했습니다.";

        // 가능하면 내려온 body 를 그대로 message 에 실어 디버깅에 활용
        if (body != null && !body.isBlank()) {
            message = message + " detail=" + body;
        }

        log.warn("FeignException 발생. status={}, body={}", rawStatus, body, e);
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, status.name(), message));
    }

    /**
     * IllegalArgumentException → 400 Bad Request
     * (검증 실패, 잘못된 파라미터 등)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "BAD_REQUEST", e.getMessage()));
    }

    /**
     * AccessDeniedException → 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "FORBIDDEN", "접근 권한이 없습니다: " + e.getMessage()));
    }

    // ==========================
    // Cart(장바구니) 도메인 예외
    // ==========================

    // Cart: 비즈니스 예외 (CartErrorCode 기반)
    @ExceptionHandler(CartException.class)
    public ResponseEntity<ErrorResponse> handleCartException(CartException ex) {
        CartErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();

        String message = (ex.getMessage() != null)
                ? ex.getMessage()
                : errorCode.getMessage();

        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, errorCode.name(), message));
    }

    // Cart: 필수 Header 누락
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "필수 요청 헤더가 누락되었습니다. detail=" + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "MISSING_HEADER", msg));
    }

    // Cart: Bean Validation (@Valid) 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");

        String msg = "요청 값 검증에 실패했습니다. detail=" + detail;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "VALIDATION_ERROR", msg));
    }

    // Cart: @Validated + @RequestParam / @PathVariable 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "요청 값 검증에 실패했습니다. detail=" + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "CONSTRAINT_VIOLATION", msg));
    }

    // Cart: JPA 낙관적 락 충돌 처리
    @ExceptionHandler({
            OptimisticLockException.class,
            ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(Exception ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        String msg = "다른 기기에서 장바구니가 변경되었습니다. 장바구니를 새로고침한 뒤 다시 시도해 주세요.";

        log.warn("cart-service에서 동시성 문제로 인한 낙관적 락 발생", ex);
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, CartErrorCode.CONCURRENCY_CONFLICT.name(), msg));
    }

    // ==========================
    // 그 외 모든 예외 (Fallback)
    // ==========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Unhandled exception", ex);

        String msg = "서버 내부 오류가 발생했습니다. detail=" + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "INTERNAL_ERROR", msg));
    }
}