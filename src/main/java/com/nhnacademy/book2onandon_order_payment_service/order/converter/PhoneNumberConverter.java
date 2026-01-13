package com.nhnacademy.book2onandon_order_payment_service.order.converter;

import com.nhnacademy.book2onandon_order_payment_service.util.AesUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
@Slf4j
public class PhoneNumberConverter implements AttributeConverter<String, String> {

    private final AesUtils aesUtils;

    @Override
    public String convertToDatabaseColumn(String attribute) {

        if(attribute == null) return null;

        return aesUtils.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {

        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        try {
            return aesUtils.decrypt(dbData);
        } catch (Exception e) {
            // 복호화 실패 시 로그를 남기고 DB에 저장된 값을 그대로 반환 (임시 방편)
            log.error("전화번호 복호화 실패: {}", dbData);
            return dbData;
        }
    }
}