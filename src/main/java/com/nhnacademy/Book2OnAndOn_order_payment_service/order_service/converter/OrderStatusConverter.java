package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.converter;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.order.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(OrderStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode(); // Enum의 code 필드 반환
    }

    @Override
    public OrderStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        // DB 코드 Enum 값으로 변환하는 로직
        return OrderStatus.fromCode(dbData);
    }
}