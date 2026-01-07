package com.nhnacademy.book2onandon_order_payment_service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    @DisplayName("ErrorResponse 객체 생성 및 필드 값 일치 검증")
    void createErrorResponseTest() {
        LocalDateTime now = LocalDateTime.now();
        int status = 400;
        String error = "BAD_REQUEST";
        String message = "잘못된 요청입니다.";

        ErrorResponse response = new ErrorResponse(now, status, error, message);

        assertThat(response.getTimestamp()).isEqualTo(now);
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getError()).isEqualTo(error);
        assertThat(response.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("필드 값이 Null이거나 비어있을 때도 객체는 정상 생성되어야 함")
    void createErrorResponseWithNullTest() {
        ErrorResponse response = new ErrorResponse(null, 0, null, null);

        assertThat(response.getTimestamp()).isNull();
        assertThat(response.getStatus()).isZero();
        assertThat(response.getError()).isNull();
        assertThat(response.getMessage()).isNull();
    }
}