package com.nhnacademy.book2onandon_order_payment_service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderVerificationExceptionTest {

    @Test
    @DisplayName("예외 생성 시 전달한 검증 실패 메시지가 정확히 유지되어야 한다")
    void exceptionMessageTest() {
        String message = "상품 가격 정보가 일치하지 않습니다.";

        OrderVerificationException exception = new OrderVerificationException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("예외가 발생했을 때 OrderVerificationException 타입으로 던져져야 한다")
    void exceptionThrowTest() {
        String message = "재고가 부족하여 주문을 생성할 수 없습니다.";

        assertThatThrownBy(() -> {
            throw new OrderVerificationException(message);
        }).isInstanceOf(OrderVerificationException.class)
          .hasMessage(message);
    }

    @Test
    @DisplayName("RuntimeException을 상속받아 트랜잭션 롤백이 가능한 비검사 예외여야 한다")
    void isRuntimeExceptionTest() {
        OrderVerificationException exception = new OrderVerificationException("verification failed");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}