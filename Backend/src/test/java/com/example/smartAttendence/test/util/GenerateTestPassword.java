package com.example.smartAttendence.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
public class GenerateTestPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "SecureAdmin2024!";
        String hash = encoder.encode(password);
        log.debug("Password: " + password);
        log.debug("BCrypt Hash: " + hash);
        
        // Test the hash
        boolean matches = encoder.matches(password, hash);
        log.debug("Hash verification: " + matches);
    }
}
