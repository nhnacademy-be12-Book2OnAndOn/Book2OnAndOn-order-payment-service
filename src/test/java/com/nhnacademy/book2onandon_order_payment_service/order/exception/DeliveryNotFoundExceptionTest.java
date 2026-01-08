package com.nhnacademy.book2onandon_order_payment_service.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryNotFoundExceptionTest {

    @Test
    @DisplayName("기본 생성자로 예외 생성 시 정해진 기본 메시지를 포함해야 한다")
    void defaultConstructorTest() {
        DeliveryNotFoundException exception = new DeliveryNotFoundException();

        assertThat(exception.getMessage()).isEqualTo("배송정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("배송 ID를 포함한 생성자 사용 시 ID 정보가 포함된 메시지를 생성해야 한다")
    void constructorWithIdTest() {
        Long deliveryId = 100L;
        DeliveryNotFoundException exception = new DeliveryNotFoundException(deliveryId);

        assertThat(exception.getMessage())
                .contains("배송정보를 찾을 수 없습니다.")
                .contains("deliveryId: 100");
    }

    @Test
    @DisplayName("커스텀 메시지를 포함한 생성자 사용 시 전달된 메시지를 정확히 유지해야 한다")
    void constructorWithMessageTest() {
        String customMessage = "특정 운송장에 대한 정보를 찾을 수 없습니다.";
        DeliveryNotFoundException exception = new DeliveryNotFoundException(customMessage);

        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}