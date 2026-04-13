package com.example.smartAttendence.util;

import java.security.SecureRandom;

/**
 * Utility class for generating secure random passwords
 * Used in user onboarding to create temporary passwords
 */
public class PasswordUtils {
    
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_CHARACTERS = UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARS;
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generates a secure random 8-character password
     * Contains uppercase, lowercase, digits, and special characters
     * 
     * @return A secure random password of 8 characters
     */
    public static String generateSecurePassword() {
        StringBuilder password = new StringBuilder(8);
        
        // Ensure at least one character from each category
        password.append(UPPER_CASE.charAt(secureRandom.nextInt(UPPER_CASE.length())));
        password.append(LOWER_CASE.charAt(secureRandom.nextInt(LOWER_CASE.length())));
        password.append(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(secureRandom.nextInt(SPECIAL_CHARS.length())));
        
        // Fill remaining positions with random characters from all categories
        String ALL_CHARACTERS = UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARS;
        for (int i = 4; i < 8; i++) {
            password.append(ALL_CHARACTERS.charAt(secureRandom.nextInt(ALL_CHARACTERS.length())));
        }
        
        // Shuffle the characters to avoid predictable patterns
        return shuffleString(password.toString(), secureRandom);
    }
    
    /**
     * Validates password strength against production standards
     */
    public static boolean validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (UPPER_CASE.indexOf(c) >= 0) hasUpper = true;
            else if (LOWER_CASE.indexOf(c) >= 0) hasLower = true;
            else if (DIGITS.indexOf(c) >= 0) hasDigit = true;
            else if (SPECIAL_CHARS.indexOf(c) >= 0) hasSpecial = true;
        }
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * Shuffles a string using the provided SecureRandom instance
     * 
     * @param input The string to shuffle
     * @param random The SecureRandom instance to use
     * @return The shuffled string
     */
    private static String shuffleString(String input, SecureRandom random) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char temp = characters[index];
            characters[index] = characters[i];
            characters[i] = temp;
        }
        return new String(characters);
    }
}
