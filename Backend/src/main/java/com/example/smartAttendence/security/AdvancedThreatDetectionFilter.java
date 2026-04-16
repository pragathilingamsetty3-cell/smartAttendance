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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔐 PRODUCTION-GRADE ADVANCED THREAT DETECTION FILTER
 */
@Component
public class AdvancedThreatDetectionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedThreatDetectionFilter.class);

    private final AdvancedInputValidator advancedInputValidator;
    private final Firestore firestore;
    private final SecurityAuditLogger auditLogger;
    private final JwtUtil jwtUtil;
    
    @Autowired
    public AdvancedThreatDetectionFilter(AdvancedInputValidator advancedInputValidator, 
                                        @Nullable Firestore firestore,
                                        SecurityAuditLogger auditLogger,
                                        JwtUtil jwtUtil) {
        this.advancedInputValidator = advancedInputValidator;
        this.firestore = firestore;
        this.auditLogger = auditLogger;
        this.jwtUtil = jwtUtil;
        if (firestore == null) {
            logger.warn("⚠️ Firestore is not initialized. Threat detection will use local caching only.");
        }
    }
    
    // 🚀 SPEED-SHIELD Caches
    private final Cache<String, Integer> threatAnalysisCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final Cache<String, AtomicInteger> rateCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    private final Cache<String, Integer> failureCountCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private final Map<String, Instant> lastRequestTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String endpoint = request.getRequestURI();
        String clientIP = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            if (isStaticOrPublicAsset(endpoint) || endpoint.startsWith("/actuator/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            // 🚀 SPEED-SHIELD check
            String cacheKey = clientIP + ":" + (userAgent != null ? userAgent.hashCode() : 0);
            Integer cachedThreatOrdinal = threatAnalysisCache.getIfPresent(cacheKey);
            if (cachedThreatOrdinal != null && cachedThreatOrdinal <= ThreatLevel.LOW.ordinal()) {
                filterChain.doFilter(request, response);
                return;
            }

            ThreatLevel level = analyzeThreat(request, clientIP, userAgent);
            
            if (level != ThreatLevel.LOW) {
                auditLogger.logSecurityEvent(level.name() + "_THREAT", clientIP, endpoint, "BLOCK", Map.of("ua", userAgent != null ? userAgent : "N/A"));
                response.setStatus(level == ThreatLevel.CRITICAL ? 403 : 429);
                response.getWriter().write("{\"error\":\"" + level.name() + " threat detected\"}");
                return;
            }

            threatAnalysisCache.put(cacheKey, level.ordinal());
        } catch (Exception e) {
            logger.error("🚨 Sentinel crash: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }

    private ThreatLevel analyzeThreat(HttpServletRequest request, String ip, String ua) {
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) return ThreatLevel.LOW;
        
        if (isSuspiciousRate(ip)) return ThreatLevel.HIGH;
        if (isSuspiciousBehavior(request, ip)) return ThreatLevel.HIGH;
        if (isSuspiciousPattern(request)) return ThreatLevel.HIGH;
        
        return ThreatLevel.LOW;
    }

    private boolean isSuspiciousRate(String ip) {
        AtomicInteger count = rateCache.get(ip, k -> new AtomicInteger(0));
        return count.incrementAndGet() > 150;
    }

    private boolean isSuspiciousBehavior(HttpServletRequest request, String ip) {
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            String userRole = null;
            String deviceFingerprint = null;
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    userRole = jwtUtil.extractRole(token);
                    deviceFingerprint = jwtUtil.extractDeviceFingerprint(token);
                } catch (Exception e) {
                    logger.warn("Failed to extract claims from token: {}", e.getMessage());
                }
            }
            
            // Check failed login attempts (all users)
            Integer fails = failureCountCache.getIfPresent(ip);
            if (fails != null && fails > 5) {
                logger.warn("Too many failed attempts from IP: {}", ip);
                return true;
            }
            
            // STRICT checks for STUDENT role only
            if ("ROLE_STUDENT".equals(userRole) || "STUDENT".equals(userRole)) {
                // Students MUST have valid device fingerprint
                String requestDeviceFingerprint = request.getHeader("X-Device-Fingerprint");
                if (requestDeviceFingerprint == null) {
                    logger.warn("Student request without device fingerprint from IP: {}", ip);
                    return true; // Block - suspicious
                }
                
                // Verify device fingerprint matches token binding
                if (deviceFingerprint != null && !deviceFingerprint.equals(requestDeviceFingerprint)) {
                    logger.warn("Device fingerprint mismatch for student from IP: {}", ip);
                    auditLogger.logSecurityEvent("DEVICE_MISMATCH_ATTEMPT", ip, request.getRequestURI(), "BLOCK", 
                        Map.of("role", "STUDENT"));
                    return true; // Block - possible stolen token
                }
            }
            
            // LIGHT checks for FACULTY/ADMIN/SUPER_ADMIN - just rate limiting
            // (IP-based rapid requests check)
            Instant now = Instant.now();
            Instant last = lastRequestTime.get(ip);
            if (last != null && now.minusSeconds(1).isBefore(last)) {
                int count = requestCounts.getOrDefault(ip, 0) + 1;
                requestCounts.put(ip, count);
                if (count > 30) { // Higher threshold for trusted roles
                    logger.warn("Rate limit exceeded for IP: {}", ip);
                    return true;
                }
            } else {
                requestCounts.put(ip, 1);
            }
            lastRequestTime.put(ip, now);
            
            return false;
        } catch (Exception e) {
            logger.error("Error analyzing suspicious behavior: {}", e.getMessage());
            return false; // Don't block on error
        }
    }

    private boolean isSuspiciousPattern(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null) return false;
        var result = advancedInputValidator.validateInput(query, "query");
        return !result.isValid();
    }

    // Removed: isKnownAdminIP - IP whitelist approach is no longer used
    // Security now based on JWT role validation and device fingerprinting

    private boolean isStaticOrPublicAsset(String e) {
        return e.contains("/css/") || e.contains("/js/") || e.endsWith(".png") || e.endsWith(".svg");
    }

    private String getClientIP(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private enum ThreatLevel { LOW, HIGH, CRITICAL }
}
