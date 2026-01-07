package com.nhnacademy.book2onandon_order_payment_service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestPasswordMismatchExceptionTest {

    @Test
    @DisplayName("예외 생성 시 전달한 메시지가 정확히 포함되어야 한다")
    void exceptionMessageTest() {
        String message = "비회원 주문 비밀번호가 일치하지 않습니다.";

        GuestPasswordMismatchException exception = new GuestPasswordMismatchException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("예외가 발생했을 때 지정된 타입으로 던져져야 한다")
    void exceptionThrowTest() {
        String message = "비밀번호 불일치";

        assertThatThrownBy(() -> {
            throw new GuestPasswordMismatchException(message);
        }).isInstanceOf(GuestPasswordMismatchException.class)
          .hasMessage(message);
    }

    @Test
    @DisplayName("상위 타입인 RuntimeException으로 캐치 가능해야 한다")
    void isRuntimeExceptionTest() {
        GuestPasswordMismatchException exception = new GuestPasswordMismatchException("error");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}