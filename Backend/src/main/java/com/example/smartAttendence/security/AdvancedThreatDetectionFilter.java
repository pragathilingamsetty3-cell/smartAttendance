package com.example.smartAttendence.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class AdvancedThreatDetectionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedThreatDetectionFilter.class);

    @Autowired
    private AdvancedInputValidator advancedInputValidator;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private HandlerInterceptor securityAuditLogger;

    private final Map<String, Instant> lastRequestTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String endpoint = request.getRequestURI();
        
        try {
            logger.info("🔍 [SENTINEL] ThreatDetectionFilter starting for: {}", endpoint);
            String clientIP = getClientIP(request);
            String userAgent = request.getHeader("User-Agent");
        
        // 🔐 SECURITY: Bypass threat detection for health checks and CORS preflight
        if (endpoint.startsWith("/actuator/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 🔐 AI-POWERED THREAT DETECTION
        ThreatLevel threatLevel = analyzeThreat(request, clientIP, userAgent);
        
        if (threatLevel == ThreatLevel.CRITICAL) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Critical threat detected - access denied\"}");
            
            // 🛡️ SAFE LOGGING - Protect against ClassCastException
            if (securityAuditLogger instanceof SecurityAuditLogger) {
                ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("CRITICAL_THREAT", clientIP, 
                    String.format("Critical threat detected: endpoint=%s, ua=%s", endpoint, userAgent));
            }
            return;
        }
        
        if (threatLevel == ThreatLevel.HIGH) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"High threat detected - rate limited\"}");
            
            // 🛡️ SAFE LOGGING - Protect against ClassCastException
            if (securityAuditLogger instanceof SecurityAuditLogger) {
                ((SecurityAuditLogger) securityAuditLogger).logSecurityEvent("HIGH_THREAT", clientIP, 
                    String.format("High threat detected: endpoint=%s, ua=%s", endpoint, userAgent));
            }
            return;
        } catch (Throwable t) {
            logger.error("🚨 [SENTINEL] ThreatDetectionFilter CRASHED but failing-open: {}", t.getMessage(), t);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private ThreatLevel analyzeThreat(HttpServletRequest request, String clientIP, String userAgent) {
        // 🔐 SECURITY: Comprehensive localhost bypass
        if ("127.0.0.1".equals(clientIP) || "0:0:0:0:0:0:0:1".equals(clientIP) || "::1".equals(clientIP)) {
            return ThreatLevel.LOW;
        }
        
        // 🔐 SECURITY: Multi-layer threat analysis
        
        // LAYER 1: RATE ANALYSIS
        if (isSuspiciousRate(clientIP)) {
            logger.warn("Suspicious rate detected for IP: {}", clientIP);
            return ThreatLevel.HIGH;
        }
        
        // LAYER 2: USER AGENT ANALYSIS
        if (isSuspiciousUserAgent(userAgent)) {
            logger.warn("Suspicious user agent detected from IP: {}", clientIP);
            return ThreatLevel.HIGH;
        }
        
        // LAYER 3: GEOLOCATION ANALYSIS
        if (isSuspiciousGeoLocation(clientIP)) {
            logger.warn("Suspicious geolocation detected for IP: {}", clientIP);
            return ThreatLevel.HIGH;
        }
        
        // LAYER 4: BEHAVIORAL ANALYSIS
        if (isSuspiciousBehavior(request, clientIP)) {
            logger.warn("Suspicious behavior detected for IP: {}", clientIP);
            return ThreatLevel.CRITICAL;
        }
        
        // LAYER 5: PATTERN ANALYSIS
        if (isSuspiciousPattern(request, clientIP)) {
            logger.warn("Suspicious pattern detected for IP: {}", clientIP);
            return ThreatLevel.HIGH;
        }
        
        return ThreatLevel.LOW;
    }
    
    private boolean isSuspiciousRate(String clientIP) {
        // 🔐 SECURITY: Bypass rate analysis for localhost
        if ("127.0.0.1".equals(clientIP) || "0:0:0:0:0:0:0:1".equals(clientIP) || "::1".equals(clientIP)) {
            return false;
        }
        
        String key = "threat_rate:" + clientIP;
        
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            
            // 🔐 MORE THAN 150 REQUESTS PER MINUTE IS SUSPICIOUS (OPTIMIZED FOR 10K USERS)
            return count > 150;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return true;
        }
        
        String lowerUA = userAgent.toLowerCase();
        
        // 🔐 CHECK FOR COMMON BOTS AND TOOLS
        String[] suspiciousUA = {
            "curl", "wget", "python", "scanner", "bot", "crawler",
            "sqlmap", "nikto", "nmap", "burp", "metasploit"
        };
        
        // 🔐 ALLOW POSTMAN FOR API TESTING
        if (lowerUA.contains("postman")) {
            return false;
        }
        
        // 🔐 ALLOW JAVA CLIENTS BUT CHECK FOR SUSPICIOUS PATTERNS
        if (lowerUA.contains("java")) {
            // Allow Java clients that are not obviously malicious
            return !lowerUA.contains("scanner") && !lowerUA.contains("bot") && !lowerUA.contains("crawler");
        }
        
        for (String suspicious : suspiciousUA) {
            if (lowerUA.contains(suspicious)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isSuspiciousGeoLocation(String clientIP) {
        // 🔐 SECURITY: Bypass geolocation checks for localhost
        if ("127.0.0.1".equals(clientIP) || "0:0:0:0:0:0:0:1".equals(clientIP) || "::1".equals(clientIP)) {
            return false;
        }
        
        // 🔐 CHECK IF IP IS FROM SUSPICIOUS LOCATIONS
        String geoKey = "threat_geo:" + clientIP;
        
        try {
            String geo = redisTemplate.opsForValue().get(geoKey);
            if (geo == null) {
                // Simplified geo lookup
                geo = getGeoLocation(clientIP);
                redisTemplate.opsForValue().set(geoKey, geo, Duration.ofHours(1));
            }
            
            // 🔐 EXTERNAL LOCATIONS ARE MORE SUSPICIOUS FOR ATTENDANCE
            return "EXTERNAL".equals(geo);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isSuspiciousBehavior(HttpServletRequest request, String clientIP) {
        // 🔐 SECURITY: Bypass behavior analysis for localhost
        if ("127.0.0.1".equals(clientIP) || "0:0:0:0:0:0:0:1".equals(clientIP) || "::1".equals(clientIP)) {
            return false;
        }
        
        // 🔐 CHECK FOR SUSPICIOUS REQUEST PATTERNS
        String endpoint = request.getRequestURI();
        String method = request.getMethod();
        
        // 🔐 MULTIPLE FAILED ATTEMPTS
        String failKey = "threat_fail:" + clientIP;
        try {
            String failCount = redisTemplate.opsForValue().get(failKey);
            if (failCount != null && Integer.parseInt(failCount) > 5) {
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        // 🔐 UNUSUAL ENDPOINT ACCESS
        if (endpoint.contains("/admin") && !isKnownAdminIP(clientIP)) {
            return true;
        }
        
        // 🔐 RAPID SUCCESSION REQUESTS
        Instant now = Instant.now();
        Instant lastTime = lastRequestTime.get(clientIP);
        if (lastTime != null && now.minusSeconds(1).isBefore(lastTime)) {
            Integer count = requestCounts.getOrDefault(clientIP, 0) + 1;
            requestCounts.put(clientIP, count);
            
            if (count > 15) { // OPTIMIZED FOR 10K USERS
                return true;
            }
        } else {
            requestCounts.put(clientIP, 1);
        }
        
        lastRequestTime.put(clientIP, now);
        return false;
    }
    
    private boolean isSuspiciousPattern(HttpServletRequest request, String clientIP) {
        // 🔐 CHECK FOR ATTACK PATTERNS
        
        // SQL INJECTION PATTERNS
        String queryString = request.getQueryString();
        if (queryString != null && containsSQLInjection(queryString)) {
            return true;
        }
        
        // XSS PATTERNS
        if (queryString != null && containsXSS(queryString)) {
            return true;
        }
        
        // PATH TRAVERSAL PATTERNS
        String path = request.getRequestURI();
        if (path.contains("../") || path.contains("..\\") || path.contains("%2e%2e")) {
            return true;
        }
        
        return false;
    }
    
    private boolean containsSQLInjection(String input) {
        // 🔐 ENHANCED SQL INJECTION DETECTION
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        // Use the advanced validator
        AdvancedInputValidator.ValidationResult result = advancedInputValidator.validateInput(input, "query");
        return !result.isValid() && result.getErrorMessage().contains("SQL injection");
    }
    
    private boolean containsXSS(String input) {
        // 🔐 ENHANCED XSS DETECTION
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        // Use the advanced validator
        AdvancedInputValidator.ValidationResult result = advancedInputValidator.validateInput(input, "query");
        return !result.isValid() && result.getErrorMessage().contains("XSS");
    }
    
    private boolean isKnownAdminIP(String clientIP) {
        // 🔐 CHECK IF IP IS IN KNOWN ADMIN LIST
        String adminKey = "admin_ips:" + clientIP;
        return redisTemplate.hasKey(adminKey);
    }
    
    private String getGeoLocation(String ip) {
        // 🔐 SIMPLIFIED GEOLOCATION
        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "CAMPUS";
        }
        return "EXTERNAL";
    }
    
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
    
    private enum ThreatLevel {
        LOW, HIGH, CRITICAL
    }
}
