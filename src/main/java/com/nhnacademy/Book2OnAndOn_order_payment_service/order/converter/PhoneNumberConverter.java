package com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter;

import com.nhnacademy.Book2OnAndOn_order_payment_service.util.AesUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
public class PhoneNumberConverter implements AttributeConverter<String, String> {

    private final AesUtils aesUtils;

    @Override
    public String convertToDatabaseColumn(String attribute) {

        if(attribute == null) return null;

        return aesUtils.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {

        if(dbData == null) return null;

        return aesUtils.decrypt(dbData);
    }
}
