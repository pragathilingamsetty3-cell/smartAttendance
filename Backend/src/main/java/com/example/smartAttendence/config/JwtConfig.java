package com.example.smartAttendence.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 🔐 JWT Configuration - Separate from SecurityConfig to avoid circular dependencies
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.public-key}")
    private String publicKeyStr;

    @Value("${jwt.private-key}")
    private String privateKeyStr;

    /**
     * 🔐 PERSISTENT RSA KEY PAIR - PREVENTS LOGOUTS ON RESTART
     */
    @Bean
    public KeyPair jwtKeyPair() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            // 🔓 RECONSTRUCT PUBLIC KEY
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr.trim());
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicBytes);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicSpec);

            // 🔐 RECONSTRUCT PRIVATE KEY
            byte[] privateBytes = Base64.getDecoder().decode(privateKeyStr.trim());
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateBytes);
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateSpec);

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("🛑 Failed to load persistent RSA keys. Check your .env file!", e);
        }
    }

    /**
     * 🔐 JWT UTILITY BEAN - RSA CONFIGURED
     */
    @Bean
    public com.example.smartAttendence.security.JwtUtil jwtUtil(KeyPair jwtKeyPair) {
        return new com.example.smartAttendence.security.JwtUtil(jwtKeyPair);
    }
}
