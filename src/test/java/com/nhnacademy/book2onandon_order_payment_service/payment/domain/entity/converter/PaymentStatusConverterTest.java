package com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentStatusConverterTest {

    private final PaymentStatusConverter converter =
            new PaymentStatusConverter();

    @Test
    @DisplayName("PaymentStatus → DB 컬럼(code) 변환")
    void convertToDatabaseColumn_success() {
        assertThat(converter.convertToDatabaseColumn(PaymentStatus.SUCCESS))
                .isZero();

        assertThat(converter.convertToDatabaseColumn(PaymentStatus.FAILURE))
                .isEqualTo(-1);

        assertThat(converter.convertToDatabaseColumn(PaymentStatus.WAITING_FOR_DEPOSIT))
                .isEqualTo(1);

        assertThat(converter.convertToDatabaseColumn(PaymentStatus.CANCEL))
                .isEqualTo(2);

        assertThat(converter.convertToDatabaseColumn(PaymentStatus.PARTIAL_CANCEL))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("DB 컬럼(code) → PaymentStatus 변환")
    void convertToEntityAttribute_success() {
        assertThat(converter.convertToEntityAttribute(0))
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(converter.convertToEntityAttribute(-1))
                .isEqualTo(PaymentStatus.FAILURE);

        assertThat(converter.convertToEntityAttribute(1))
                .isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);

        assertThat(converter.convertToEntityAttribute(2))
                .isEqualTo(PaymentStatus.CANCEL);

        assertThat(converter.convertToEntityAttribute(3))
                .isEqualTo(PaymentStatus.PARTIAL_CANCEL);
    }

    @Test
    @DisplayName("존재하지 않는 코드 변환 시 예외 발생")
    void convertToEntityAttribute_invalid_code() {
        assertThatThrownBy(() ->
                converter.convertToEntityAttribute(999))
                .isInstanceOf(EnumConstantNotPresentException.class);
    }
}
