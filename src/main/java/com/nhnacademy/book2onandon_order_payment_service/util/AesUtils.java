package com.nhnacademy.book2onandon_order_payment_service.util;

import com.nhnacademy.book2onandon_order_payment_service.exception.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesUtils {

    @Value("${encryption.secret-key}")
    private String secretKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new EncryptionException("암호화 실패", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            if (decoded.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid cipher text length");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] encryptedContent = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedContent);

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

            return new String(cipher.doFinal(encryptedContent), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new EncryptionException("복호화 실패", e);
        }
    }
}