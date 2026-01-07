package com.nhnacademy.book2onandon_order_payment_service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DuplicateExceptionTest {

    @Test
    @DisplayName("예외 발생 시 전달한 메시지가 정확히 유지되어야 한다 (Happy Path)")
    void exceptionMessageTest() {
        String message = "중복된 데이터가 존재합니다.";

        DuplicateException exception = new DuplicateException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("실제 throw 키워드와 함께 사용 시 런타임 예외로 잡혀야 한다 (Happy Path)")
    void exceptionThrowTest() {
        try {
            throw new DuplicateException("Duplicate error");
        } catch (RuntimeException e) {
            assertThat(e).isInstanceOf(DuplicateException.class);
            assertThat(e.getMessage()).isEqualTo("Duplicate error");
        }
    }
}