package com.example.smartAttendence.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Simple BCrypt hash generator for test passwords
 */
@Slf4j
public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(15);
        String password = "SecurePassword123!";
        String hash = encoder.encode(password);
        log.debug("Password: " + password);
        log.debug("BCrypt-15 Hash: " + hash);
        log.debug("Length: " + hash.length());
    }
}
