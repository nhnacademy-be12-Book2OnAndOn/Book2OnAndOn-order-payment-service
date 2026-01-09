package com.nhnacademy.book2onandon_order_payment_service.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestOrderNotFoundExceptionTest {

    @Test
    @DisplayName("기본 생성자로 예외 생성 시 정의된 기본 메시지를 반환해야 한다")
    void defaultConstructorTest() {
        GuestOrderNotFoundException exception = new GuestOrderNotFoundException();

        assertThat(exception.getMessage()).isEqualTo("비회원 주문정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("커스텀 메시지를 인자로 받는 생성자 사용 시 해당 메시지를 유지해야 한다")
    void constructorWithMessageTest() {
        String customMessage = "요청하신 주문 번호에 해당하는 비회원 주문이 존재하지 않습니다.";
        GuestOrderNotFoundException exception = new GuestOrderNotFoundException(customMessage);

        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}