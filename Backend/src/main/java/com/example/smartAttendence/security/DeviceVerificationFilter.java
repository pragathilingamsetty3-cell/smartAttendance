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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.security.MessageDigest;
import java.util.Arrays;

@Component
public class DeviceVerificationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceVerificationFilter.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private Cache<String, Boolean> deviceVerificationCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String endpoint = request.getRequestURI();
        String clientIP = getClientIP(request);
        
        try {
            logger.info("🔍 [SENTINEL] DeviceFilter starting for: {}", endpoint);
            // 🔐 BYPASS DEVICE VERIFICATION FOR LOCALHOST, PUBLIC ENDPOINTS AND CORS PREFLIGHT
        if (isLocalhost(clientIP) || !requiresDeviceVerification(endpoint) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String deviceFingerprint = generateDeviceFingerprint(request);
        String sessionId = request.getHeader("X-Session-ID");

        // 🚀 SPEED-SHIELD: Check local memory cache first
        String cacheKey = deviceFingerprint + ":" + sessionId;
        if (Boolean.TRUE.equals(deviceVerificationCache.getIfPresent(cacheKey))) {
            logger.debug("⚡ [SPEED-SHIELD] Device verification SKIPPED (Local Memory hit) for Session: {}", sessionId);
            filterChain.doFilter(request, response);
            return;
        }
            
            if (sessionId == null || sessionId.isEmpty()) {
                // 🔐 SMART FALLBACK: Extract from JWT if header is missing
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.substring(7);
                        sessionId = jwtUtil.extractSessionId(token);
                        logger.info("🔍 [SENTINEL] DeviceFilter found SessionID in JWT: {}", sessionId);
                    } catch (Exception e) {
                        logger.warn("⚠️ [SENTINEL] DeviceFilter could not extract SessionID from JWT");
                    }
                }
            }
            
            if (!isDeviceVerified(deviceFingerprint, sessionId)) {
                logger.warn("🚨 [SENTINEL] Device verification FAILED for IP: {} (SessionID: {})", clientIP, sessionId);
                response.setStatus(401);
                response.getWriter().write("{\"error\":\"Device verification failed\"}");
                return;
            }

            // ✅ [SPEED-SHIELD] Passed. Cache the verification for 5 minutes.
            deviceVerificationCache.put(cacheKey, true);
        } catch (Throwable t) {
            logger.error("🚨 [SENTINEL] DeviceFilter CRASHED but failing-open: {}", t.getMessage(), t);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean requiresDeviceVerification(String endpoint) {
        return endpoint.contains("/attendance/check-in") || 
               endpoint.contains("/admin/") ||
               endpoint.contains("/faculty/");
    }
    
    private String generateDeviceFingerprint(HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            String acceptLanguage = request.getHeader("Accept-Language");
            String acceptEncoding = request.getHeader("Accept-Encoding");
            String accept = request.getHeader("Accept");
            
            String fingerprintData = userAgent + "|" + acceptLanguage + "|" + acceptEncoding + "|" + accept;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprintData.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            return "unknown-device";
        }
    }
    
    private boolean isDeviceVerified(String deviceFingerprint, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        
        String key = "device_session:" + sessionId;
        
        try {
            String storedFingerprint = redisTemplate.opsForValue().get(key);
            
            if (storedFingerprint == null) {
                // 🔐 NEW DEVICE - REGISTER WITH SESSION
                redisTemplate.opsForValue().set(key, deviceFingerprint, Duration.ofHours(24));
                return true;
            }
            
            // 🔐 VERIFY FINGERPRINT MATCHES
            return storedFingerprint.equals(deviceFingerprint);
            
        } catch (Exception e) {
            // 🔐 FAIL SECURE - IF REDIS FAILS, ALLOW
            return true;
        }
    }
    
    private boolean isLocalhost(String clientIP) {
        return "127.0.0.1".equals(clientIP) || 
               "0:0:0:0:0:0:0:1".equals(clientIP) || 
               "::1".equals(clientIP) ||
               clientIP.startsWith("192.168.") ||
               clientIP.startsWith("10.") ||
               clientIP.startsWith("172.");
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
