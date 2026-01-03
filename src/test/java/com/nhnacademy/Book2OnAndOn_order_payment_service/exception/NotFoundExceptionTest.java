package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotFoundExceptionTest {

    @Test
    @DisplayName("예외 생성 시 입력한 메시지가 정확히 포함되어야 한다")
    void exceptionMessageTest() {
        String message = "해당 리소스를 찾을 수 없습니다.";

        NotFoundException exception = new NotFoundException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("예외 발생 시 지정된 타입으로 정확히 던져져야 한다")
    void exceptionThrowTest() {
        String message = "Resource Not Found";

        assertThatThrownBy(() -> {
            throw new NotFoundException(message);
        }).isInstanceOf(NotFoundException.class)
          .hasMessage(message);
    }

    @Test
    @DisplayName("RuntimeException을 상속받아 비검사 예외로 동작해야 한다")
    void isRuntimeExceptionTest() {
        NotFoundException exception = new NotFoundException("error");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}