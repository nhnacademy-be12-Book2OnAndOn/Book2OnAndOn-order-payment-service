package com.nhnacademy.Book2OnAndOn_order_payment_service.payment.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TossPaymentsPropertiesTest {

    @Test
    @DisplayName("Setter를 통해 설정한 값이 Getter를 통해 정확히 반환되는지 확인한다")
    void secretKeyGetterSetterTest() {
        TossPaymentsProperties properties = new TossPaymentsProperties();
        String testSecretKey = "test_sk_zG0rJ62Gz7Kn64N0ro3eb5edD";

        properties.setSecretKey(testSecretKey);

        assertThat(properties.getSecretKey()).isEqualTo(testSecretKey);
    }
}