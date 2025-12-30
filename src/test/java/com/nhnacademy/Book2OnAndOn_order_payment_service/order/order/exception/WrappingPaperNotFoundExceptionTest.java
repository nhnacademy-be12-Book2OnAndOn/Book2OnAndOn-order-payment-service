package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.exception.WrappingPaperNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WrappingPaperNotFoundExceptionTest {

    @Test
    @DisplayName("기본 생성자로 예외 생성 시 설정된 기본 메시지를 반환한다")
    void defaultConstructorTest() {
        WrappingPaperNotFoundException exception = new WrappingPaperNotFoundException();

        assertThat(exception.getMessage()).isEqualTo("포장지 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("포장지 ID를 포함한 생성자 사용 시 해당 ID가 명시된 메시지를 생성한다")
    void constructorWithIdTest() {
        Long wrappingPaperId = 7L;
        WrappingPaperNotFoundException exception = new WrappingPaperNotFoundException(wrappingPaperId);

        assertThat(exception.getMessage())
                .contains("포장지 정보를 찾을 수 없습니다.")
                .contains("(ID: 7)");
    }

    @Test
    @DisplayName("RuntimeException의 하위 클래스인지 확인한다")
    void instanceOfTest() {
        WrappingPaperNotFoundException exception = new WrappingPaperNotFoundException();
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}