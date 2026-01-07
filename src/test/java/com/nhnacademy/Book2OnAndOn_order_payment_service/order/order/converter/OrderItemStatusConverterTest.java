package com.nhnacademy.Book2OnAndOn_order_payment_service.order.order.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter.OrderItemStatusConverter;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.order.OrderItemStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderItemStatusConverterTest {

    private final OrderItemStatusConverter converter = new OrderItemStatusConverter();

    @Test
    @DisplayName("Enum 값을 DB 컬럼 값(Integer)으로 정상 변환한다")
    void convertToDatabaseColumnTest() {
        OrderItemStatus status = OrderItemStatus.PENDING;
        Integer expectedCode = status.getCode();

        Integer result = converter.convertToDatabaseColumn(status);

        assertThat(result).isEqualTo(expectedCode);
    }

    @Test
    @DisplayName("DB 컬럼 값이 null일 경우 null을 반환한다")
    void convertToDatabaseColumn_NullTest() {
        Integer result = converter.convertToDatabaseColumn(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DB의 Integer 코드를 Enum 상수로 정상 변환한다")
    void convertToEntityAttributeTest() {
        OrderItemStatus expectedStatus = OrderItemStatus.DELIVERED;
        Integer dbData = expectedStatus.getCode();

        OrderItemStatus result = converter.convertToEntityAttribute(dbData);

        assertThat(result).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("DB 데이터가 null일 경우 Enum도 null을 반환한다")
    void convertToEntityAttribute_NullTest() {
        OrderItemStatus result = converter.convertToEntityAttribute(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 코드가 들어올 경우 예외가 발생해야 한다")
    void convertToEntityAttribute_InvalidCodeTest() {
        Integer invalidCode = -999;

        assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidCode))
                .isInstanceOf(IllegalArgumentException.class);
    }
}