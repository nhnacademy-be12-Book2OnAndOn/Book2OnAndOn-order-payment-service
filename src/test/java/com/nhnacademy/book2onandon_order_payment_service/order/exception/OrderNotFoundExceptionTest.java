package com.nhnacademy.book2onandon_order_payment_service.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.book2onandon_order_payment_service.order.exception.OrderNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderNotFoundExceptionTest {

    @Test
    @DisplayName("기본 생성자로 예외 생성 시 설정된 기본 메시지를 반환한다")
    void defaultConstructorTest() {
        OrderNotFoundException exception = new OrderNotFoundException();

        assertThat(exception.getMessage()).isEqualTo("주문을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("주문 ID를 포함한 생성자 사용 시 해당 ID가 명시된 메시지를 생성한다")
    void constructorWithIdTest() {
        Long orderId = 12345L;
        OrderNotFoundException exception = new OrderNotFoundException(orderId);

        assertThat(exception.getMessage())
                .contains("주문을 찾을 수 없습니다.")
                .contains("(ID: 12345)");
    }

    @Test
    @DisplayName("커스텀 메시지를 인자로 받는 생성자 사용 시 해당 메시지를 그대로 유지한다")
    void constructorWithMessageTest() {
        String customMessage = "요청하신 주문 번호에 해당하는 내역이 존재하지 않습니다.";
        OrderNotFoundException exception = new OrderNotFoundException(customMessage);

        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}