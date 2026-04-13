package com.example.smartAttendence.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Production Password Hash Generator for Testing
 * 
 * This utility generates secure BCrypt hashes for test passwords
 * that comply with production security requirements.
 */
@Slf4j
public class ProductionPasswordGenerator {
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    
    public static void main(String[] args) {
        // Generate hashes for production-compliant test passwords
        String[] testPasswords = {
            "Test@SecurePass2024!",
            "Admin@Production2024!",
            "Faculty@Secure2024!",
            "Student@Pass2024!"
        };
        
        log.debug("=== PRODUCTION PASSWORD HASH GENERATOR ===");
        log.debug("BCrypt Strength: 10 rounds");
        log.debug("");
        
        for (String password : testPasswords) {
            String hash = encoder.encode(password);
            boolean verification = encoder.matches(password, hash);
            
            log.debug("Password: " + password);
            log.debug("Hash: " + hash);
            log.debug("Verified: " + verification);
            log.debug("---");
        }
        
        // Test a very unique password for maximum security
        String uniquePassword = "Qwerty@2024!Production";
        String uniqueHash = encoder.encode(uniquePassword);
        log.debug("UNIQUE PASSWORD: " + uniquePassword);
        log.debug("UNIQUE HASH: " + uniqueHash);
        log.debug("VERIFICATION: " + encoder.matches(uniquePassword, uniqueHash));
    }
}
