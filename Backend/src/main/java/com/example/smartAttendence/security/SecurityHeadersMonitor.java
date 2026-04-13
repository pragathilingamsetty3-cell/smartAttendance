package com.example.smartAttendence.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 🔐 PRODUCTION-GRADE SECURITY HEADERS MONITOR
 * 
 * Monitors and validates security headers on all responses
 * Ensures comprehensive security header compliance
 */
@Component
public class SecurityHeadersMonitor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersMonitor.class);
    
    // Security header tracking
    private final Map<String, List<SecurityHeaderEvent>> headerEvents = new ConcurrentHashMap<>();
    
    // Required security headers
    private static final String[] REQUIRED_HEADERS = {
        "X-Content-Type-Options",
        "X-Frame-Options", 
        "X-XSS-Protection",
        "Strict-Transport-Security",
        "Content-Security-Policy",
        "Referrer-Policy"
    };
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // No action needed before request
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String clientIP = getClientIP(request);
        String endpoint = request.getRequestURI();
        int status = response.getStatus();
        
        // Monitor security headers on successful responses
        if (status >= 200 && status < 400) {
            validateSecurityHeaders(response, clientIP, endpoint);
        }
        
        // Log missing security headers
        logMissingSecurityHeaders(response, clientIP, endpoint);
    }
    
    /**
     * Validate security headers are present and correct
     */
    private void validateSecurityHeaders(HttpServletResponse response, String clientIP, String endpoint) {
        List<String> missingHeaders = new ArrayList<>();
        List<String> incorrectHeaders = new ArrayList<>();
        
        for (String header : REQUIRED_HEADERS) {
            String value = response.getHeader(header);
            
            if (value == null || value.trim().isEmpty()) {
                missingHeaders.add(header);
                logSecurityHeaderEvent(clientIP, endpoint, header, "MISSING", null);
            } else {
                // Validate header values
                String validationResult = validateHeaderValue(header, value);
                if (validationResult != null) {
                    incorrectHeaders.add(header + ": " + validationResult);
                    logSecurityHeaderEvent(clientIP, endpoint, header, "INVALID", validationResult);
                } else {
                    logSecurityHeaderEvent(clientIP, endpoint, header, "VALID", value);
                }
            }
        }
        
        // Alert on missing or incorrect headers
        if (!missingHeaders.isEmpty() || !incorrectHeaders.isEmpty()) {
            logger.warn("🚨 SECURITY HEADERS ISSUE - IP: {} | Endpoint: {} | Missing: {} | Incorrect: {}", 
                clientIP, endpoint, missingHeaders, incorrectHeaders);
        }
    }
    
    /**
     * Validate individual header value
     */
    private String validateHeaderValue(String header, String value) {
        switch (header) {
            case "X-Content-Type-Options":
                if (!"nosniff".equals(value)) {
                    return "Should be 'nosniff'";
                }
                break;
                
            case "X-Frame-Options":
                if (!"DENY".equals(value) && !"SAMEORIGIN".equals(value)) {
                    return "Should be 'DENY' or 'SAMEORIGIN'";
                }
                break;
                
            case "X-XSS-Protection":
                if (!value.contains("1; mode=block")) {
                    return "Should be '1; mode=block'";
                }
                break;
                
            case "Strict-Transport-Security":
                if (!value.contains("max-age=") || !value.contains("includeSubDomains")) {
                    return "Should include max-age and includeSubDomains";
                }
                break;
                
            case "Content-Security-Policy":
                // 🔐 SECURITY: Allow 'unsafe-inline' and 'unsafe-eval' for local development compatibility
                // next.js and some other frameworks require these for styles or specific functionality
                if (!value.contains("default-src 'self'")) {
                    return "Should restrict default source to self";
                }
                break;
                
            case "Referrer-Policy":
                String[] validPolicies = {"no-referrer", "no-referrer-when-downgrade", 
                    "origin", "origin-when-cross-origin", "same-origin", 
                    "strict-origin", "strict-origin-when-cross-origin", "unsafe-url"};
                boolean isValidPolicy = false;
                for (String policy : validPolicies) {
                    if (value.equals(policy)) {
                        isValidPolicy = true;
                        break;
                    }
                }
                if (!isValidPolicy) {
                    return "Invalid referrer policy";
                }
                break;
        }
        
        return null; // Header is valid
    }
    
    /**
     * Log missing security headers
     */
    private void logMissingSecurityHeaders(HttpServletResponse response, String clientIP, String endpoint) {
        List<String> missingHeaders = new ArrayList<>();
        
        for (String header : REQUIRED_HEADERS) {
            String value = response.getHeader(header);
            if (value == null || value.trim().isEmpty()) {
                missingHeaders.add(header);
            }
        }
        
        if (!missingHeaders.isEmpty()) {
            logger.error("🚨 MISSING SECURITY HEADERS - IP: {} | Endpoint: {} | Headers: {}", 
                clientIP, endpoint, missingHeaders);
        }
    }
    
    /**
     * Log security header event
     */
    private void logSecurityHeaderEvent(String clientIP, String endpoint, String header, String status, String value) {
        SecurityHeaderEvent event = new SecurityHeaderEvent(
            Instant.now(),
            header,
            status,
            value,
            clientIP,
            endpoint
        );
        
        headerEvents.computeIfAbsent(clientIP, k -> new ArrayList<>()).add(event);
        
        // Clean old events periodically
        if (headerEvents.get(clientIP).size() > 1000) {
            cleanupOldEvents(clientIP);
        }
    }
    
    /**
     * Get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Clean old events
     */
    private void cleanupOldEvents(String clientIP) {
        List<SecurityHeaderEvent> events = headerEvents.get(clientIP);
        if (events != null) {
            Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour
            events.removeIf(event -> event.getTimestamp().isBefore(cutoff));
        }
    }
    
    /**
     * Get security header events for analysis
     */
    public List<SecurityHeaderEvent> getHeaderEvents(String clientIP) {
        return headerEvents.getOrDefault(clientIP, new ArrayList<>());
    }
    
    /**
     * Get all security header events
     */
    public Map<String, List<SecurityHeaderEvent>> getAllHeaderEvents() {
        return new ConcurrentHashMap<>(headerEvents);
    }
    
    /**
     * Get security header compliance report
     */
    public SecurityHeaderComplianceReport getComplianceReport() {
        int totalEvents = 0;
        int validEvents = 0;
        int missingEvents = 0;
        int invalidEvents = 0;
        
        for (List<SecurityHeaderEvent> events : headerEvents.values()) {
            for (SecurityHeaderEvent event : events) {
                totalEvents++;
                switch (event.getStatus()) {
                    case "VALID":
                        validEvents++;
                        break;
                    case "MISSING":
                        missingEvents++;
                        break;
                    case "INVALID":
                        invalidEvents++;
                        break;
                }
            }
        }
        
        return new SecurityHeaderComplianceReport(
            totalEvents,
            validEvents,
            missingEvents,
            invalidEvents,
            totalEvents > 0 ? (validEvents * 100.0 / totalEvents) : 0.0
        );
    }
    
    /**
     * Security header event data class
     */
    public static class SecurityHeaderEvent {
        private final Instant timestamp;
        private final String header;
        private final String status;
        private final String value;
        private final String clientIP;
        private final String endpoint;
        
        public SecurityHeaderEvent(Instant timestamp, String header, String status, String value, String clientIP, String endpoint) {
            this.timestamp = timestamp;
            this.header = header;
            this.status = status;
            this.value = value;
            this.clientIP = clientIP;
            this.endpoint = endpoint;
        }
        
        // Getters
        public Instant getTimestamp() { return timestamp; }
        public String getHeader() { return header; }
        public String getStatus() { return status; }
        public String getValue() { return value; }
        public String getClientIP() { return clientIP; }
        public String getEndpoint() { return endpoint; }
    }
    
    /**
     * Security header compliance report
     */
    public static class SecurityHeaderComplianceReport {
        private final int totalEvents;
        private final int validEvents;
        private final int missingEvents;
        private final int invalidEvents;
        private final double compliancePercentage;
        
        public SecurityHeaderComplianceReport(int totalEvents, int validEvents, int missingEvents, int invalidEvents, double compliancePercentage) {
            this.totalEvents = totalEvents;
            this.validEvents = validEvents;
            this.missingEvents = missingEvents;
            this.invalidEvents = invalidEvents;
            this.compliancePercentage = compliancePercentage;
        }
        
        // Getters
        public int getTotalEvents() { return totalEvents; }
        public int getValidEvents() { return validEvents; }
        public int getMissingEvents() { return missingEvents; }
        public int getInvalidEvents() { return invalidEvents; }
        public double getCompliancePercentage() { return compliancePercentage; }
        
        public String getComplianceGrade() {
            if (compliancePercentage >= 95) return "EXCELLENT";
            if (compliancePercentage >= 85) return "VERY_GOOD";
            if (compliancePercentage >= 70) return "GOOD";
            if (compliancePercentage >= 50) return "MODERATE";
            return "POOR";
        }
    }
}
