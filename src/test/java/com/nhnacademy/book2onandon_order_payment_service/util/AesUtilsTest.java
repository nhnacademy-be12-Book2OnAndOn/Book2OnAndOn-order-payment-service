package com.nhnacademy.book2onandon_order_payment_service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AesUtilsTest {

    private AesUtils aesUtils;
    private final String testSecretKey = "1234567890123456";
    @BeforeEach
    void setUp() {
        aesUtils = new AesUtils();
        ReflectionTestUtils.setField(aesUtils, "secretKey", testSecretKey);
    }

    @Test
    @DisplayName("평문을 암호화하면 원본과 다른 Base64 문자열이 생성되어야 한다")
    void encrypt_Success() {
        String plainText = "010-1234-5678";

        String encryptedText = aesUtils.encrypt(plainText);

        assertThat(encryptedText).isNotBlank();
        assertThat(encryptedText).isNotEqualTo(plainText);
    }

    @Test
    @DisplayName("암호화된 텍스트를 복호화하면 원래의 평문이 복구되어야 한다")
    void decrypt_Success() {
        String plainText = "Hello Book2OnAndOn";
        String encryptedText = aesUtils.encrypt(plainText);

        String decryptedText = aesUtils.decrypt(encryptedText);

        assertThat(decryptedText).isEqualTo(plainText);
    }

    @Test
    @DisplayName("잘못된 비밀키 형식이거나 암호화 로직 오류 시 RuntimeException이 발생한다")
    void encrypt_Failure_InvalidKey() {
        ReflectionTestUtils.setField(aesUtils, "secretKey", "short");

        assertThatThrownBy(() -> aesUtils.encrypt("test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("암호화 실패");
    }

    @Test
    @DisplayName("손상된 암호문이나 잘못된 Base64 형식을 복호화할 경우 예외가 발생한다")
    void decrypt_Failure_InvalidCipherText() {
        String invalidCipherText = "not-a-base64-string!";

        assertThatThrownBy(() -> aesUtils.decrypt(invalidCipherText))
                .isInstanceOf(RuntimeException.class);
    }
}