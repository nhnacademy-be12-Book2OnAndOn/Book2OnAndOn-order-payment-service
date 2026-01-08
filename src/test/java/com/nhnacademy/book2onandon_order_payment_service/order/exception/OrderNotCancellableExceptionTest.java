package com.nhnacademy.book2onandon_order_payment_service.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNotCancellableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderNotCancellableExceptionTest {

    @Test
    @DisplayName("커스텀 메시지를 전달하여 예외 생성 시 해당 메시지가 정확히 유지되어야 한다")
    void constructorWithMessageTest() {
        String errorMessage = "이미 배송 중인 주문은 취소할 수 없습니다.";
        
        OrderNotCancellableException exception = new OrderNotCancellableException(errorMessage);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}