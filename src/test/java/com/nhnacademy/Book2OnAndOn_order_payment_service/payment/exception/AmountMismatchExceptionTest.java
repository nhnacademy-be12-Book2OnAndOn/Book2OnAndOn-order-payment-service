package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AmountMismatchExceptionTest {

    @Test
    @DisplayName("커스텀 메시지를 전달하여 예외 생성 시 해당 메시지가 정확히 유지되어야 한다")
    void constructorWithMessageTest() {
        String errorMessage = "결제 요청 금액(10000)과 실제 결제 금액(9000)이 일치하지 않습니다.";
        
        AmountMismatchException exception = new AmountMismatchException(errorMessage);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}