package com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter;

import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.return1.ReturnStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * ReturnStatus Enum과 DB의 TINYINT(Integer) 타입 간의 변환을 담당합니다.
 */
@Converter(autoApply = true)
public class ReturnStatusConverter implements AttributeConverter<ReturnStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ReturnStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public ReturnStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return ReturnStatus.fromCode(dbData);
    }
}