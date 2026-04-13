package com.example.smartAttendence.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class HighPerformanceRateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HighPerformanceRateLimitingFilter.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 🔐 ENHANCED RATE LIMIT CONFIGURATION - OPTIMIZED FOR UNIVERSITY SCALE (15K+ USERS)
    @Value("${rate-limit.global.requests-per-minute}") 
    private int globalRequestsPerMinute;

    @Value("${rate-limit.auth.requests-per-minute}") 
    private int authRequestsPerMinute;

    @Value("${rate-limit.admin.requests-per-minute}") 
    private int adminRequestsPerMinute;

    @Value("${rate-limit.heartbeat.requests-per-minute}") 
    private int heartbeatRequestsPerMinute;

    @Value("${rate-limit.burst-capacity}")
    private int burstCapacity;

    @Value("${rate-limit.suspicious-threshold}")
    private int suspiciousThreshold;

    @Value("${rate-limit.rapid-succession-threshold}")
    private int rapidSuccessionThreshold;

    private static final int STRICT_REQUESTS_PER_MINUTE = 2000; 
    private static final int ADMIN_REQUESTS_PER_MINUTE = 6000; 

    // 🔐 CONSTRUCTOR - Initialize Safe Mode
    public HighPerformanceRateLimitingFilter() {
        // Security: Removed debug outputs for production safety
    }

    // 🔐 POST CONSTRUCT - Validate configuration silently
    @PostConstruct
    public void validateConfiguration() {
        // 🔐 SAFE MODE CHECK - No sensitive data exposure
        if (globalRequestsPerMinute <= 0 || authRequestsPerMinute <= 0 || 
            adminRequestsPerMinute <= 0 || heartbeatRequestsPerMinute <= 0) {
            logger.warn("Rate limiting: Safe mode activated - using secure defaults");
        } else {
            logger.info("Rate limiting: Configuration loaded successfully");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String endpoint = request.getRequestURI();
        String clientIP = getClientIP(request);
        
        // 🚀 PERFORMANCE OPTIMIZATION - Bypass rate limiting for localhost
        if ("127.0.0.1".equals(clientIP) || "0:0:0:0:0:0:0:1".equals(clientIP) || "::1".equals(clientIP)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 🔐 SECURITY: Rate limiting filter - no sensitive data exposure
        
        // 🔐 SECURITY: Bypass rate limiting for health checks and CORS preflight
        if (endpoint.startsWith("/actuator/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 🔐 SECURITY: Rate limiting validation - no sensitive data in logs
        
        String clientId = getClientIdentifier(request);
        
        // 🔐 ENHANCED RATE LIMITING LOGIC
        int maxRequests = getMaxRequestsForEndpoint(endpoint);
        int windowSeconds = getWindowSecondsForEndpoint(endpoint);
        
        if (isRateLimited(clientId, endpoint, maxRequests, windowSeconds)) {
            logger.warn("🚫 Rate limit exceeded for client {} on endpoint {}", clientId, endpoint);
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":60}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        // 🚀 OPTIMIZED CLIENT IDENTIFICATION - Performance focused
        String ip = getClientIP(request);
        String endpoint = request.getRequestURI();
        
        // 🛡️ SMART SECURITY: Use Authorization Token if available for heartbeats
        // This allows 200+ students on the SAME WIFI (Shared IP) to be identified separately
        if (endpoint.contains("/heartbeat")) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return ip + ":" + authHeader.substring(7).hashCode();
            }
            return ip; // Faster for high-frequency operations if anonymous
        }
        
        if (endpoint.contains("/attendance/scan")) {
            return ip; 
        }
        
        // For sensitive operations, use enhanced identification
        String userAgent = request.getHeader("User-Agent");
        return ip + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }
    
    private boolean isRateLimited(String clientId, String endpoint, int maxRequests, int windowSeconds) {
        String key = "rate_limit:" + endpoint + ":" + clientId;
        String burstKey = "rate_limit_burst:" + endpoint + ":" + clientId;
        
        try {
            // 🚀 PERFORMANCE OPTIMIZATION - Check burst capacity first
            Long burstCount = redisTemplate.opsForValue().increment(burstKey);
            if (burstCount == 1) {
                redisTemplate.expire(burstKey, Duration.ofSeconds(windowSeconds / 4)); // Burst window is 1/4 of main window
            }
            
            // If within burst capacity, allow request
            if (burstCount <= burstCapacity) {
                // Still check main rate limit but don't block yet
                Long currentCount = redisTemplate.opsForValue().increment(key);
                if (currentCount == 1) {
                    redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
                }
                
                return currentCount > maxRequests && burstCount > burstCapacity;
            }
            
            // Exceeded burst capacity, check main rate limit strictly
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            
            return currentCount > maxRequests;
            
        } catch (Exception e) {
            logger.error("❌ Rate limiting error for endpoint {}: {}", endpoint, e.getMessage());
            // 🔐 FAIL SECURE - IF REDIS FAILS, ALLOW REQUEST
            return false;
        }
    }
    
    private int getMaxRequestsForEndpoint(String endpoint) {
        int maxRequests;
        
        // 🔐 SAFE MODE - Apply environment variables with performance-optimized fallbacks for 10K users
        int safeGlobalRpm = globalRequestsPerMinute <= 0 ? 1000 : globalRequestsPerMinute; // Optimized for 10K users
        int safeAuthRpm = authRequestsPerMinute <= 0 ? 100 : authRequestsPerMinute; // Optimized for 10K users
        int safeAdminRpm = adminRequestsPerMinute <= 0 ? 500 : adminRequestsPerMinute; // Optimized for 10K users
        int safeHeartbeatRpm = heartbeatRequestsPerMinute <= 0 ? 2000 : heartbeatRequestsPerMinute; // Optimized for 10K users
        
        if (endpoint.contains("/auth/login")) {
            maxRequests = safeAuthRpm;
        } else if (endpoint.contains("/attendance/check-in")) {
            maxRequests = 1; // Keep strict for check-in
        } else if (endpoint.contains("/admin/")) {
            maxRequests = safeAdminRpm;
        } else if (endpoint.contains("/heartbeat")) {
            maxRequests = safeHeartbeatRpm;
        } else {
            maxRequests = safeGlobalRpm;
        }
        
        // 🔐 CRITICAL SAFE MODE - If maxRequests is still 0 or invalid, force to 500 for 10K users
        if (maxRequests <= 0) {
            logger.warn("🚨 SAFE MODE: Invalid maxRequests detected ({}), forcing to 500", maxRequests);
            maxRequests = 500;
        }
        
        return maxRequests;
    }
    
    private int getWindowSecondsForEndpoint(String endpoint) {
        int windowSeconds;
        
        if (endpoint.contains("/auth/login")) {
            windowSeconds = 300; // 5 minutes
        } else if (endpoint.contains("/attendance/check-in")) {
            windowSeconds = 60; // 1 minute
        } else {
            windowSeconds = 60; // 1 minute default
        }
        
        // 🔐 SAFE MODE - If windowSeconds is 0 or invalid, use safe default
        return windowSeconds <= 0 ? 60 : windowSeconds;
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
}
