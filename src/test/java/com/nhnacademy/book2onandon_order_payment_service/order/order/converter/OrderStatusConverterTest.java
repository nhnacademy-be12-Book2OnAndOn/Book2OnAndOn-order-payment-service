package com.nhnacademy.book2onandon_order_payment_service.order.order.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhnacademy.book2onandon_order_payment_service.order.converter.OrderStatusConverter;
import com.nhnacademy.book2onandon_order_payment_service.order.entity.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderStatusConverterTest {

    private final OrderStatusConverter converter = new OrderStatusConverter();

    @Test
    @DisplayName("OrderStatus Enum 상수를 DB에 저장할 Integer 코드로 변환한다")
    void convertToDatabaseColumn_Success() {
        OrderStatus status = OrderStatus.PENDING;
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
    @DisplayName("DB에서 읽어온 Integer 코드를 OrderStatus Enum 상수로 변환한다")
    void convertToEntityAttribute_Success() {
        OrderStatus expectedStatus = OrderStatus.DELIVERED;
        Integer dbData = expectedStatus.getCode();

        OrderStatus result = converter.convertToEntityAttribute(dbData);

        assertThat(result).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("읽어온 DB 데이터가 null이면 null을 반환한다")
    void convertToEntityAttribute_Null() {
        OrderStatus result = converter.convertToEntityAttribute(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DB에 정의되지 않은 잘못된 코드가 들어올 경우 예외가 발생한다 (Fail Path)")
    void convertToEntityAttribute_Fail_InvalidCode() {
        Integer invalidCode = 9999;

        assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidCode))
                .isInstanceOf(IllegalArgumentException.class);
    }
}