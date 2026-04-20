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

            ThreatAnalysisResult analysis = analyzeThreat(request, clientIP, userAgent);
            
            if (analysis.getLevel() != ThreatLevel.LOW) {
                auditLogger.logSecurityEvent(analysis.getReason(), clientIP, endpoint, "BLOCK", Map.of("ua", userAgent != null ? userAgent : "N/A"));
                
                int status = (analysis.getLevel() == ThreatLevel.CRITICAL) ? 403 : 429;
                response.setStatus(status);
                response.setContentType("application/json");
                
                String errorMessage = analysis.getFriendlyMessage();
                response.getWriter().write("{\"error\":\"" + errorMessage + "\"}");
                return;
            }

            threatAnalysisCache.put(cacheKey, analysis.getLevel().ordinal());
        } catch (Exception e) {
            logger.error("🚨 Sentinel crash: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }

    private ThreatAnalysisResult analyzeThreat(HttpServletRequest request, String ip, String ua) {
        String endpoint = request.getRequestURI();
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) 
            return new ThreatAnalysisResult(ThreatLevel.LOW, "LOCAL_BYPASS", "");
        
        if (isSuspiciousRate(ip)) 
            return new ThreatAnalysisResult(ThreatLevel.HIGH, "RATE_LIMIT_EXCEEDED", "Too many requests. Please wait a moment.");
            
        String behaviorReason = checkSuspiciousBehavior(request, ip, endpoint);
        if (behaviorReason != null) {
            String friendlyMsg = behaviorReason.contains("DEVICE_MISMATCH") ? 
                "You can't login multiple devices. Please use the same device or contact admin." : 
                "Security check failed. Please ensure you are using a registered device.";
            return new ThreatAnalysisResult(ThreatLevel.HIGH, behaviorReason, friendlyMsg);
        }

        if (isSuspiciousPattern(request)) 
            return new ThreatAnalysisResult(ThreatLevel.HIGH, "MALICIOUS_PATTERN", "Request blocked due to security policies.");
        
        return new ThreatAnalysisResult(ThreatLevel.LOW, "NORMAL", "");
    }

    private boolean isSuspiciousRate(String ip) {
        AtomicInteger count = rateCache.get(ip, k -> new AtomicInteger(0));
        return count.incrementAndGet() > 150;
    }

    private String checkSuspiciousBehavior(HttpServletRequest request, String ip, String endpoint) {
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
                return "TOO_MANY_FAILED_LOGINS";
            }
            
            // STRICT checks for STUDENT role only
            if ("ROLE_STUDENT".equals(userRole) || "STUDENT".equals(userRole)) {
                // 🛡️ EXEMPTION: Allow setup and password change without header during first-time setup
                boolean isSetupFlow = endpoint.endsWith("/complete-setup") || endpoint.endsWith("/change-password");
                
                // Students MUST have valid device fingerprint (except during setup)
                String requestDeviceFingerprint = request.getHeader("X-Device-Fingerprint");
                if (requestDeviceFingerprint == null && !isSetupFlow) {
                    logger.warn("Student request without device fingerprint from IP: {} at {}", ip, endpoint);
                    return "STUDENT_MISSING_FINGERPRINT"; 
                }
                
                // Verify device fingerprint matches token binding
                if (deviceFingerprint != null && requestDeviceFingerprint != null && !deviceFingerprint.equals(requestDeviceFingerprint)) {
                    logger.warn("Device fingerprint mismatch for student from IP: {} at {}", ip, endpoint);
                    auditLogger.logSecurityEvent("DEVICE_MISMATCH_ATTEMPT", ip, endpoint, "BLOCK", 
                        Map.of("role", "STUDENT"));
                    return "DEVICE_MISMATCH_STUDENT"; 
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
                    return "RAPID_REQUESTS_BURST";
                }
            } else {
                requestCounts.put(ip, 1);
            }
            lastRequestTime.put(ip, now);
            
            return null;
        } catch (Exception e) {
            logger.error("Error analyzing suspicious behavior: {}", e.getMessage());
            return null; 
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

    private static class ThreatAnalysisResult {
        private final ThreatLevel level;
        private final String reason;
        private final String friendlyMessage;

        public ThreatAnalysisResult(ThreatLevel level, String reason, String friendlyMessage) {
            this.level = level;
            this.reason = reason;
            this.friendlyMessage = friendlyMessage;
        }

        public ThreatLevel getLevel() { return level; }
        public String getReason() { return reason; }
        public String getFriendlyMessage() { return friendlyMessage; }
    }
}
