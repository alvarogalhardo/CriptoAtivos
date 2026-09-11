package com.criptoativos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class EncryptionConfig {

    /**
     * AES-256 for TOTP secrets at rest, so a database dump on its own cannot be used to mint valid
     * one-time codes.
     */
    @Bean
    TextEncryptor textEncryptor(
            @Value("${app.security.encryption.password}") String password,
            @Value("${app.security.encryption.salt}") String salt) {
        return Encryptors.delux(password, salt);
    }
}
