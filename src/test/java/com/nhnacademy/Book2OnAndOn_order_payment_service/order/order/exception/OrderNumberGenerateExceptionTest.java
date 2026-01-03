package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.OrderNumberGenerateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderNumberGenerateExceptionTest {

    @Test
    @DisplayName("메시지만 전달하여 예외 생성 시 메시지가 올바르게 저장된다")
    void constructorWithMessageTest() {
        String message = "주문 번호 생성 중 오류가 발생했습니다.";
        
        OrderNumberGenerateException exception = new OrderNumberGenerateException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("메시지와 원인 예외(Throwable)를 함께 전달하여 생성 시 모두 유지된다")
    void constructorWithMessageAndCauseTest() {
        String message = "Redis 연결 실패로 인한 주문 번호 생성 불가";
        Throwable cause = new RuntimeException("Connection Timeout");

        OrderNumberGenerateException exception = new OrderNumberGenerateException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}