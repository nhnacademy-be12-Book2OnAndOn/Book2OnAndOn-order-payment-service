package com.nhnacademy.book2onandon_order_payment_service.payment.property;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TossPaymentsPropertiesTest {

    @Test
    @DisplayName("setter/getter로 secretKey 값 확인")
    void setGet_test() {
        // 객체 생성
        TossPaymentsProperties properties = new TossPaymentsProperties();

        // 값 세팅
        properties.setSecretKey("test-secret-key");

        // 값 확인
        assertThat(properties.getSecretKey()).isEqualTo("test-secret-key");
    }
}
