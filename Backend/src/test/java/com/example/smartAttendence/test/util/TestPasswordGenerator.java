package com.example.smartAttendence.test.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Test Password Generator Utility
 * 
 * This utility generates BCrypt-15 hashes for testing purposes.
 * All test users use the same password: "SecurePassword123!"
 * 
 * Password: SecurePassword123!
 * BCrypt Strength: 15 (Maximum security)
 * Algorithm: BCrypt
 */
@Slf4j
public class TestPasswordGenerator {

    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final int BCRYPT_STRENGTH = 15;
    
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    
    /**
     * Generate BCrypt-15 hash for the test password
     * 
     * @return BCrypt-15 hash of "SecurePassword123!"
     */
    public static String generateTestPasswordHash() {
        return passwordEncoder.encode(TEST_PASSWORD);
    }
    
    /**
     * Get the plain test password (for documentation purposes)
     * 
     * @return Plain test password
     */
    public static String getTestPassword() {
        return TEST_PASSWORD;
    }
    
    /**
     * Verify a password against the test hash
     * 
     * @param password Password to verify
     * @param hash Hash to verify against
     * @return True if password matches
     */
    public static boolean verifyPassword(String password, String hash) {
        return passwordEncoder.matches(password, hash);
    }
    
    /**
     * Main method for generating hashes during development
     */
    public static void main(String[] args) {
        String hash = generateTestPasswordHash();
        log.debug("Test Password: " + TEST_PASSWORD);
        log.debug("BCrypt-15 Hash: " + hash);
        log.debug("Hash Length: " + hash.length());
        log.debug("Verification: " + verifyPassword(TEST_PASSWORD, hash));
    }
}
