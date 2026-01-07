package com.nhnacademy.book2onandon_order_payment_service.util;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AesUtils {

    @Value("${encryption.secret-key}")
    private String secretKey;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    public String encrypt(String plainText) {
        try{
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
