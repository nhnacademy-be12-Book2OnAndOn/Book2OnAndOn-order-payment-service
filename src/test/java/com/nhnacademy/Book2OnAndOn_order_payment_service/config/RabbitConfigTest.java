package com.nhnacademy.Book2OnAndOn_order_payment_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RabbitConfigTest {

    @Test
    @DisplayName("RabbitMQ 설정에 정의된 상수값들이 올바른지 확인한다")
    void rabbitConstantsTest() {
        assertThat(RabbitConfig.EXCHANGE).isEqualTo("book2.dev.order-payment.exchange");
        assertThat(RabbitConfig.ROUTING_KEY_CONFIRM_BOOK).isEqualTo("book.confirm");
        assertThat(RabbitConfig.ROUTING_KEY_CANCEL_BOOK).isEqualTo("book.cancel");
        assertThat(RabbitConfig.ROUTING_KEY_CANCEL_COUPON).isEqualTo("coupon.cancel");
        assertThat(RabbitConfig.ROUTING_KEY_CANCEL_POINT).isEqualTo("point.cancel");
    }

    @Test
    @DisplayName("설정 클래스의 인스턴스가 정상적으로 생성되는지 확인한다")
    void configInstanceTest() {
        RabbitConfig config = new RabbitConfig();
        assertThat(config).isNotNull();
    }
}