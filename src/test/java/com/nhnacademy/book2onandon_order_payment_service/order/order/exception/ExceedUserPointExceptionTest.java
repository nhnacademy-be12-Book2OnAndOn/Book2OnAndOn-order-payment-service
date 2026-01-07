package com.nhnacademy.book2onandon_order_payment_service.order.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.book2onandon_order_payment_service.order.exception.ExceedUserPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExceedUserPointExceptionTest {

    @Test
    @DisplayName("커스텀 메시지를 전달하여 예외 생성 시 해당 메시지가 정확히 유지되어야 한다")
    void constructorWithMessageTest() {
        String errorMessage = "사용 가능한 포인트를 초과하였습니다. (보유 포인트: 5000)";
        
        ExceedUserPointException exception = new ExceedUserPointException(errorMessage);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}