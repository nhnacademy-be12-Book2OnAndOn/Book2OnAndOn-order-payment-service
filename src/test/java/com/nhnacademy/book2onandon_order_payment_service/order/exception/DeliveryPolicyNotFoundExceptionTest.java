package com.nhnacademy.book2onandon_order_payment_service.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryPolicyNotFoundExceptionTest {

    @Test
    @DisplayName("기본 생성자로 예외 생성 시 설정된 기본 메시지를 반환한다")
    void defaultConstructorTest() {
        DeliveryPolicyNotFoundException exception = new DeliveryPolicyNotFoundException();

        assertThat(exception.getMessage()).isEqualTo("배송정책 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("배송 정책 ID를 포함한 생성자 사용 시 해당 ID가 포함된 메시지를 생성한다")
    void constructorWithIdTest() {
        Long policyId = 50L;
        DeliveryPolicyNotFoundException exception = new DeliveryPolicyNotFoundException(policyId);

        assertThat(exception.getMessage())
                .contains("배송정보를 찾을 수 없습니다.")
                .contains("deliveryPolicyId: 50");
    }

    @Test
    @DisplayName("커스텀 메시지를 인자로 받는 생성자 사용 시 해당 메시지를 그대로 유지한다")
    void constructorWithMessageTest() {
        String customMessage = "유효한 배송 정책이 존재하지 않습니다.";
        DeliveryPolicyNotFoundException exception = new DeliveryPolicyNotFoundException(customMessage);

        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}