package com.nhnacademy.book2onandon_order_payment_service.order.order.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.nhnacademy.book2onandon_order_payment_service.order.converter.PhoneNumberConverter;
import com.nhnacademy.book2onandon_order_payment_service.util.AesUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhoneNumberConverterTest {

    private AesUtils aesUtils;
    private PhoneNumberConverter converter;

    @BeforeEach
    void setUp() {
        aesUtils = mock(AesUtils.class);
        converter = new PhoneNumberConverter(aesUtils);
    }

    @Test
    @DisplayName("전화번호를 DB에 저장할 때 암호화하여 반환한다")
    void convertToDatabaseColumn_Success() {
        String rawPhone = "010-1234-5678";
        String encryptedPhone = "encrypted-phone-data";
        given(aesUtils.encrypt(rawPhone)).willReturn(encryptedPhone);

        String result = converter.convertToDatabaseColumn(rawPhone);

        assertThat(result).isEqualTo(encryptedPhone);
    }

    @Test
    @DisplayName("DB에 저장할 전화번호가 null이면 null을 반환한다 (Fail Path)")
    void convertToDatabaseColumn_Null() {
        String result = converter.convertToDatabaseColumn(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("DB에서 가져온 암호화된 번호를 복호화하여 엔티티에 반환한다")
    void convertToEntityAttribute_Success() {
        String encryptedPhone = "encrypted-phone-data";
        String decryptedPhone = "010-1234-5678";
        given(aesUtils.decrypt(encryptedPhone)).willReturn(decryptedPhone);

        String result = converter.convertToEntityAttribute(encryptedPhone);

        assertThat(result).isEqualTo(decryptedPhone);
    }

    @Test
    @DisplayName("DB에서 읽어온 데이터가 null이면 null을 반환한다")
    void convertToEntityAttribute_Null() {
        String result = converter.convertToEntityAttribute(null);

        assertThat(result).isNull();
    }
}