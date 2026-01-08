package com.nhnacademy.book2onandon_order_payment_service.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNumberProvisionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderNumberProvisionExceptionTest {

    @Test
    @DisplayName("메시지만 전달하여 예외 생성 시 메시지가 정확히 저장되어야 한다")
    void constructorWithMessageTest() {
        String message = "주문 번호 할당에 실패했습니다.";

        OrderNumberProvisionException exception = new OrderNumberProvisionException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("메시지와 원인 예외를 함께 전달하여 생성 시 모든 정보가 유지되어야 한다")
    void constructorWithMessageAndCauseTest() {
        String message = "네트워크 오류로 주문 번호를 가져올 수 없습니다.";
        Throwable cause = new IllegalStateException("Connection closed");

        OrderNumberProvisionException exception = new OrderNumberProvisionException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}