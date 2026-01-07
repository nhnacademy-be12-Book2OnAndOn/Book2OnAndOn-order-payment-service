
package com.nhnacademy.book2onandon_order_payment_service.payment.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DuplicatePaymentExceptionTest {

    @Test
    @DisplayName("커스텀 메시지를 전달하여 예외 생성 시 해당 메시지가 정확히 유지되어야 한다")
    void constructorWithMessageTest() {
        String errorMessage = "이미 결제가 완료된 주문입니다. (주문번호: ORD-12345)";
        
        DuplicatePaymentException exception = new DuplicatePaymentException(errorMessage);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}