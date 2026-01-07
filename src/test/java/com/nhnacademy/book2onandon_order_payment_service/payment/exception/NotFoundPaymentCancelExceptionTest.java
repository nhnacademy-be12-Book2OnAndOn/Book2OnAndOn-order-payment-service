package com.nhnacademy.book2onandon_order_payment_service.payment.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotFoundPaymentCancelExceptionTest {

    @Test
    @DisplayName("커스텀 메시지를 전달하여 예외 생성 시 해당 메시지가 정확히 유지되어야 한다")
    void constructorWithMessageTest() {
        String errorMessage = "해당 결제 취소 내역을 찾을 수 없습니다. (CancelID: CAN_999)";
        
        NotFoundPaymentCancelException exception = new NotFoundPaymentCancelException(errorMessage);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}