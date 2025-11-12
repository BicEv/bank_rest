package com.example.bankcards.util;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.bankcards.config.EncryptionConfig;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;

public class CardNumberConverter implements AttributeConverter<String, String> {

    private static EncryptionConfig staticConfig;

    @Autowired
    private EncryptionConfig encryptionConfig;

    @PostConstruct
    public void init() {
        staticConfig = encryptionConfig;
    }

    public static void setStaticConfig(EncryptionConfig encryptionConfig) {
        staticConfig = encryptionConfig;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null)
            return null;

        try {
            Cipher cipher = Cipher.getInstance(staticConfig.getAlgorithm());
            SecretKeySpec key = new SecretKeySpec(staticConfig.getSecretKey().getBytes(), staticConfig.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting card number", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null)
            return null;

        try {
            Cipher cipher = Cipher.getInstance(staticConfig.getAlgorithm());
            SecretKeySpec key = new SecretKeySpec(staticConfig.getSecretKey().getBytes(), staticConfig.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting card number", e);
        }
    }

}
