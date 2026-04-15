package com.example.smartAttendence.security;

import com.example.smartAttendence.security.AdvancedInputValidator.ValidationResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 🔐 PRODUCTION-GRADE REQUEST VALIDATION FILTER
 * 
 * Comprehensive input validation for all API endpoints
 * Prevents injection attacks and ensures data integrity
 */
@Component
public class RequestValidationFilter {
    
    private final AdvancedInputValidator advancedInputValidator;
    
    // 🚀 PERFORMANCE OPTIMIZATION - Local in-memory cache for validation results
    private final Cache<String, String> validationCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();
    
    @Autowired
    public RequestValidationFilter(AdvancedInputValidator advancedInputValidator) {
        this.advancedInputValidator = advancedInputValidator;
    }
    
    /**
     * Validate incoming request parameters with caching for performance
     */
    public ValidationResult validateRequest(Map<String, Object> requestData, String endpoint) {
        // 🔓 BYPASS STRICT VALIDATION FOR LOGIN ENDPOINT
        if (endpoint != null && endpoint.contains("/auth/login")) {
            return ValidationResult.valid();
        }
        
        // 🚀 PERFORMANCE OPTIMIZATION - Check local cache first
        String cacheKey = generateValidationCacheKey(requestData, endpoint);
        String cachedResult = validationCache.getIfPresent(cacheKey);
        
        if (cachedResult != null) {
            if ("VALID".equals(cachedResult)) return ValidationResult.valid();
            return ValidationResult.invalid(List.of(cachedResult.split("\\|")));
        }
        
        List<String> violations = new ArrayList<>();
        
        if (requestData == null || requestData.isEmpty()) {
            ValidationResult result = ValidationResult.valid();
            cacheValidationResult(cacheKey, result);
            return result;
        }
        
        // Validate each parameter
        for (Map.Entry<String, Object> entry : requestData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null) {
                ValidationResult result = validateParameterValue(key, value.toString(), endpoint);
                if (!result.isValid()) {
                    violations.addAll(result.getViolations());
                }
            }
        }
        
        ValidationResult finalResult = violations.isEmpty() ? 
            ValidationResult.valid() : 
            ValidationResult.invalid(violations);
        
        // Cache the result for future requests
        cacheValidationResult(cacheKey, finalResult);
        
        return finalResult;
    }
    
    private ValidationResult validateParameterValue(String paramName, String value, String endpoint) {
        if (isSafeParameter(paramName, endpoint)) return ValidationResult.valid();
        
        ValidationResult result = advancedInputValidator.validateInput(value, paramName);
        
        if (endpoint.contains("/boundaries")) return validateBoundaryData(paramName, value, result);
        if (endpoint.contains("/auth")) return validateAuthData(paramName, value, result);
        
        return result;
    }
    
    private ValidationResult validateBoundaryData(String paramName, String value, ValidationResult currentResult) {
        List<String> violations = new ArrayList<>(currentResult.getViolations());
        if (paramName.toLowerCase().contains("lat") || paramName.toLowerCase().contains("lng")) {
            try {
                double coord = Double.parseDouble(value);
                if (paramName.toLowerCase().contains("lat")) {
                    if (coord < -90 || coord > 90) violations.add("Latitude must be between -90 and 90");
                } else {
                    if (coord < -180 || coord > 180) violations.add("Longitude must be between -180 and 180");
                }
            } catch (NumberFormatException e) {
                violations.add(paramName + " must be a valid number");
            }
        }
        return violations.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(violations);
    }
    
    private ValidationResult validateAuthData(String paramName, String value, ValidationResult currentResult) {
        List<String> violations = new ArrayList<>(currentResult.getViolations());
        if (paramName.toLowerCase().contains("email")) {
            ValidationResult emailResult = advancedInputValidator.validateEmail(value);
            if (!emailResult.isValid()) violations.addAll(emailResult.getViolations());
        }
        if (paramName.toLowerCase().contains("password")) return ValidationResult.valid(); 
        return violations.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(violations);
    }
    
    private boolean isSafeParameter(String paramName, String endpoint) {
        String[] safeParams = {"id", "uuid", "timestamp", "version", "status", "active", "page", "size", "sort", "order", "limit", "offset"};
        for (String safe : safeParams) {
            if (paramName.toLowerCase().equals(safe)) return true;
        }
        return false;
    }
    
    public Map<String, Object> sanitizeRequestData(Map<String, Object> requestData) {
        if (requestData == null || requestData.isEmpty()) return new HashMap<>();
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : requestData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            sanitized.put(key, (value instanceof String) ? advancedInputValidator.sanitizeInput((String) value) : value);
        }
        return sanitized;
    }
    
    private String generateValidationCacheKey(Map<String, Object> requestData, String endpoint) {
        try {
            StringBuilder dataBuilder = new StringBuilder(endpoint);
            if (requestData != null) {
                requestData.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> dataBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("|"));
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "vcache:" + hexString.toString();
        } catch (Exception e) {
            return "vcache:" + endpoint + "_" + Objects.hashCode(requestData);
        }
    }
    
    private void cacheValidationResult(String cacheKey, ValidationResult result) {
        String value = result.isValid() ? "VALID" : String.join("|", result.getViolations());
        validationCache.put(cacheKey, value);
    }
}
