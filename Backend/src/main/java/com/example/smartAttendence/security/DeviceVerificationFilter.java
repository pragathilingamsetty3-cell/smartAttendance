package com.example.smartAttendence.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.cloud.firestore.Firestore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class DeviceVerificationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceVerificationFilter.class);

    private final Firestore firestore;
    private final JwtUtil jwtUtil;
    
    // ⚡ SPEED-SHIELD: Local in-memory cache to avoid Firestore hits on every request
    private final Cache<String, Boolean> speedShieldCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Autowired
    public DeviceVerificationFilter(Firestore firestore, JwtUtil jwtUtil) {
        this.firestore = firestore;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String endpoint = request.getRequestURI();
        String clientIP = getClientIP(request);
        
        try {
            if (isLocalhost(clientIP) || !requiresDeviceVerification(endpoint) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }
            
            String deviceFingerprint = generateDeviceFingerprint(request);
            String sessionId = extractSessionId(request);

            if (sessionId == null) {
                logger.warn("🚨 [SENTINEL] Device verification FAILED: No SessionID found for IP: {}", clientIP);
                response.setStatus(401);
                response.getWriter().write("{\"error\":\"Session mapping required\"}");
                return;
            }

            // 🚀 SPEED-SHIELD check (Local)
            String cacheKey = deviceFingerprint + ":" + sessionId;
            if (Boolean.TRUE.equals(speedShieldCache.getIfPresent(cacheKey))) {
                filterChain.doFilter(request, response);
                return;
            }
            
            if (!isDeviceVerified(deviceFingerprint, sessionId)) {
                logger.warn("🚨 [SENTINEL] Device verification FAILED for IP: {} (Session: {})", clientIP, sessionId);
                response.setStatus(401);
                response.getWriter().write("{\"error\":\"Device mismatch detected\"}");
                return;
            }

            // ✅ Passed. Cache locally for 5 minutes.
            speedShieldCache.put(cacheKey, true);
            
        } catch (Exception e) {
            logger.error("🚨 [SENTINEL] DeviceFilter crash: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }

    private String extractSessionId(HttpServletRequest request) {
        String sessionId = request.getHeader("X-Session-ID");
        if (sessionId != null && !sessionId.isEmpty()) return sessionId;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractSessionId(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        return null;
    }
    
    private boolean isDeviceVerified(String fingerprint, String sessionId) {
        try {
            var docRef = firestore.collection("device_sessions").document(sessionId);
            var snapshot = docRef.get().get();

            if (!snapshot.exists()) {
                // 🔐 NEW DEVICE - REGISTER
                docRef.set(Map.of("fingerprint", fingerprint, "registeredAt", com.google.cloud.Timestamp.now()));
                return true;
            }
            
            return fingerprint.equals(snapshot.getString("fingerprint"));
        } catch (Exception e) {
            return true; // Fail open
        }
    }

    private boolean requiresDeviceVerification(String endpoint) {
        return endpoint.contains("/attendance/check-in") || endpoint.contains("/admin/") || endpoint.contains("/faculty/");
    }
    
    private String generateDeviceFingerprint(HttpServletRequest request) {
        try {
            String data = request.getHeader("User-Agent") + "|" + request.getHeader("Accept");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("192.168.");
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
