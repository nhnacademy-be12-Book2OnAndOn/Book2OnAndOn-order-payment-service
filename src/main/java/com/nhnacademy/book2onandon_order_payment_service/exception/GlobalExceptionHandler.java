package com.nhnacademy.book2onandon_order_payment_service.exception;

import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartBusinessException;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartErrorCode;
import com.nhnacademy.book2onandon_order_payment_service.cart.exception.CartException;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.RefundNotCancelableException;
import com.nhnacademy.book2onandon_order_payment_service.order.exception.RefundOrderMismatchException;
import com.nhnacademy.book2onandon_order_payment_service.payment.exception.AmountMismatchException;
import feign.FeignException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
// Spring 6+ (Boot 3.x)에서 @Validated 계열이 이 예외로 떨어질 수 있음
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_FAIL_MSG_PREFIX = "요청 값 검증에 실패했습니다. detail=";

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
            status = HttpStatus.BAD_GATEWAY;
        }

        String body = e.contentUTF8();
        String message = "외부 API 호출 중 오류가 발생했습니다.";
        if (body != null && !body.isBlank()) {
            message = message + " detail=" + body;
        }

        log.warn("FeignException 발생. status={}, body={}", rawStatus, body, e);
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, status.name(), message));
    }

    /**
     * IllegalArgumentException → 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "BAD_REQUEST", e.getMessage()));
    }

    /**
     * AccessDeniedException → 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "FORBIDDEN", "접근 권한이 없습니다: " + e.getMessage()));
    }

    @ExceptionHandler({
            RefundOrderMismatchException.class,
            RefundNotCancelableException.class
    })
    public ResponseEntity<ErrorResponse> handleRefundBusinessBadRequest(RuntimeException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "BAD_REQUEST", e.getMessage()));
    }

    // ==========================
    // Cart(장바구니) 도메인 예외
    // ==========================

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

     @ExceptionHandler(CartBusinessException.class)
     public ResponseEntity<ErrorResponse> handleCartBusinessException(CartBusinessException ex) {
         CartErrorCode errorCode = ex.getErrorCode();
         HttpStatus status = errorCode.getHttpStatus();

         String message = (ex.getMessage() != null)
                 ? ex.getMessage()
                 : errorCode.getMessage();

         return ResponseEntity.status(status)
                  .body(buildErrorResponse(status, errorCode.name(), message));
     }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "필수 요청 헤더가 누락되었습니다. detail=" + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "MISSING_HEADER", msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");

        String msg = VALIDATION_FAIL_MSG_PREFIX + detail;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = VALIDATION_FAIL_MSG_PREFIX + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "CONSTRAINT_VIOLATION", msg));
    }

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
    // Web / Spring MVC 바인딩·검증 예외
    // ==========================

    /**
     * PathVariable/RequestParam 타입 변환 실패 (ex: /items/not-a-number)
     * → 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "요청 값 타입이 올바르지 않습니다. detail=" + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "TYPE_MISMATCH", msg));
    }

    /**
     * JSON 바디 파싱 실패 / 바디 누락
     * → 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "요청 본문(JSON)을 읽을 수 없습니다. detail=" + ex.getMostSpecificCause().getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "MESSAGE_NOT_READABLE", msg));
    }

    /**
     * @ModelAttribute 바인딩 실패 등 (폼/쿼리 바인딩에서 자주 발생)
     * → 400
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Binding failed");

        String msg = "요청 값 바인딩에 실패했습니다. detail=" + detail;
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "BIND_ERROR", msg));
    }

    /**
     * Spring 6+ 메서드 파라미터 검증(@Validated) 실패가 이 예외로 떨어질 수 있음
     * → 400
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = VALIDATION_FAIL_MSG_PREFIX + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "VALIDATION_ERROR", msg));
    }

    /**
     * 지원하지 않는 HTTP 메서드
     * → 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        String msg = "지원하지 않는 HTTP 메서드입니다. detail=" + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "METHOD_NOT_ALLOWED", msg));
    }

    /**
     * 지원하지 않는 Content-Type
     * → 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        String msg = "지원하지 않는 Content-Type 입니다. detail=" + ex.getMessage();
        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, "UNSUPPORTED_MEDIA_TYPE", msg));
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
