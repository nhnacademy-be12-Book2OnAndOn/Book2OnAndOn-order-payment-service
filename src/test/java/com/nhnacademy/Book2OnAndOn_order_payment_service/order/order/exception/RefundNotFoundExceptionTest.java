package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.RefundNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundNotFoundExceptionTest {

    @Test
    @DisplayName("커스텀 메시지를 전달하여 예외 생성 시 메시지가 정확히 유지되어야 한다")
    void constructorWithMessageTest() {
        String errorMessage = "해당 주문에 대한 환불 내역을 찾을 수 없습니다.";
        
        RefundNotFoundException exception = new RefundNotFoundException(errorMessage);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}