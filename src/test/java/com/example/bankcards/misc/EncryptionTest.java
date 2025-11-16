package com.example.bankcards.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.bankcards.config.EncryptionConfig;
import com.example.bankcards.util.CardNumberConverter;

public class EncryptionTest {

    @Test
    void testEncryption() {
        EncryptionConfig config = new EncryptionConfig();
        config.setAlgorithm("AES");
        config.setSecretKey("MySuperSecretKey");

        CardNumberConverter converter = new CardNumberConverter();
        converter.setStaticConfig(config);
        

        String plain = "1234567890123456";
        String encrypted = converter.convertToDatabaseColumn(plain);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        System.out.println("Encrypted: " + encrypted);
        System.out.println("Decrypted: " + decrypted);

        assertEquals(plain, decrypted);
    }

}
