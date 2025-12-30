package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.NotSupportedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotSupportedExceptionTest {

    @Test
    @DisplayName("예외 생성 시 입력한 메시지가 부모 클래스로 정확히 전달되어야 한다")
    void exceptionMessageTest() {
        String message = "지원하지 않는 결제 수단입니다.";

        NotSupportedException exception = new NotSupportedException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("예외 발생 시 NotSupportedException 타입으로 정확히 던져져야 한다")
    void exceptionThrowTest() {
        String message = "Not Supported Operation";

        assertThatThrownBy(() -> {
            throw new NotSupportedException(message);
        }).isInstanceOf(NotSupportedException.class)
          .hasMessage(message);
    }

    @Test
    @DisplayName("RuntimeException의 하위 클래스로서 비검사 예외로 동작해야 한다")
    void isRuntimeExceptionTest() {
        NotSupportedException exception = new NotSupportedException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}