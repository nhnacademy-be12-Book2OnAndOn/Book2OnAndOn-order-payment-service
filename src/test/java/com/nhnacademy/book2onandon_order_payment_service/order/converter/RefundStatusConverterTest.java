package com.nhnacademy.book2onandon_order_payment_service.order.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhnacademy.book2onandon_order_payment_service.order.converter.RefundStatusConverter;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.refund.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundStatusConverterTest {

    private final RefundStatusConverter converter = new RefundStatusConverter();

    @Test
    @DisplayName("RefundStatus Enum을 DB 컬럼 값인 Integer로 정상 변환한다")
    void convertToDatabaseColumn_Success() {
        RefundStatus status = RefundStatus.REFUND_COMPLETED;
        Integer expectedCode = status.getCode();

        Integer result = converter.convertToDatabaseColumn(status);

        assertThat(result).isEqualTo(expectedCode);
    }

    @Test
    @DisplayName("DB에 저장할 Enum 값이 null이면 null을 반환한다")
    void convertToDatabaseColumn_Null() {
        Integer result = converter.convertToDatabaseColumn(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DB에서 읽어온 Integer 코드를 RefundStatus Enum으로 정상 변환한다")
    void convertToEntityAttribute_Success() {
        RefundStatus expectedStatus = RefundStatus.REQUESTED;
        Integer dbData = expectedStatus.getCode();

        RefundStatus result = converter.convertToEntityAttribute(dbData);

        assertThat(result).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("읽어온 DB 데이터가 null이면 null을 반환한다")
    void convertToEntityAttribute_Null() {
        RefundStatus result = converter.convertToEntityAttribute(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DB에 정의되지 않은 잘못된 코드가 들어올 경우 예외가 발생한다")
    void convertToEntityAttribute_Fail_InvalidCode() {
        Integer invalidCode = -1;

        assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidCode))
                .isInstanceOf(IllegalArgumentException.class);
    }
}