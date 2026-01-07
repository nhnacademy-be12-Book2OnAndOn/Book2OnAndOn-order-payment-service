package com.nhnacademy.book2onandon_order_payment_service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentExceptionTest {

    @Test
    @DisplayName("예외 생성 시 전달한 결제 실패 메시지가 정확히 유지되어야 한다")
    void exceptionMessageTest() {
        String message = "결제 승인 과정에서 오류가 발생했습니다.";

        PaymentException exception = new PaymentException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("예외가 실제로 던져졌을 때 PaymentException 타입으로 포착되어야 한다")
    void exceptionThrowTest() {
        String message = "결제 금액 불일치";

        assertThatThrownBy(() -> {
            throw new PaymentException(message);
        }).isInstanceOf(PaymentException.class)
          .hasMessage(message);
    }

    @Test
    @DisplayName("RuntimeException을 상속받아 별도의 예외 처리가 강제되지 않아야 한다")
    void isRuntimeExceptionTest() {
        PaymentException exception = new PaymentException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}