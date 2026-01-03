package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.InvalidDeliveryDateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvalidDeliveryDateExceptionTest {

    @Test
    @DisplayName("커스텀 메시지를 전달하여 예외 생성 시 메시지가 올바르게 저장되어야 한다")
    void constructorWithMessageTest() {
        String errorMessage = "희망 배송일은 현재 날짜보다 이전일 수 없습니다.";
        
        InvalidDeliveryDateException exception = new InvalidDeliveryDateException(errorMessage);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}