package com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.converter;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order_service.entity.order.OrderItemStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrderItemStatusConverter implements AttributeConverter<OrderItemStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(OrderItemStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public OrderItemStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return OrderItemStatus.fromCode(dbData);
    }
}