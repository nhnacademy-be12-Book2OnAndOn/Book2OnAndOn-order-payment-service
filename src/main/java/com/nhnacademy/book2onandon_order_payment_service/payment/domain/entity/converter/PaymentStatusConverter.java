package com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.converter;

import com.nhnacademy.book2onandon_order_payment_service.payment.domain.entity.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(PaymentStatus paymentStatus) {
        return paymentStatus.getCode();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(Integer code) {
        return PaymentStatus.getEnum(code);
    }
}
