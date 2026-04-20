package com.example.smartAttendence.security;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SecurityAuditLogger implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditLogger.class);

    private final Firestore firestore;

    @Autowired
    public SecurityAuditLogger(@Nullable Firestore firestore) {
        this.firestore = firestore;
    }

    private final Map<String, AtomicLong> securityMetrics = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();
    private final Map<String, List<SecurityEvent>> securityEvents = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastFailedAttempt = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAttemptCount = new ConcurrentHashMap<>();
    private final Map<String, Instant> recentEvents = new ConcurrentHashMap<>();
    
    private static final List<String> SUSPICIOUS_ENDPOINTS = Arrays.asList("/api/v1/users/admin", "/api/v1/system/config");
    private static final List<String> ATTACK_INDICATORS = Arrays.asList("sql", "script", "select", "drop", "union", "--", "/*", "powershell");
    
    private static final int MAX_RESPONSE_TIME_MS = 500;
    private static final int MAX_RECENT_EVENTS = 10000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIP = getClientIP(request);
        String endpoint = request.getRequestURI();
        long startTime = System.currentTimeMillis();
        
        // ⚡ OPTIMIZATION: Only log suspicious requests to Firestore synchronously
        // Standard requests are tracked only if they are slow (in postHandle)
        if (isSuspiciousRequest(request, clientIP)) {
            handleSuspiciousActivity(request, clientIP);
        }
        
        if (isBruteForceAttempt(clientIP)) {
            handleBruteForceAttempt(request, clientIP);
        }
        
        request.setAttribute("security_start_time", startTime);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        Long startTime = (Long) request.getAttribute("security_start_time");
        if (startTime != null) recordSecurityEvent("REQUEST_PROCESSING", System.currentTimeMillis() - startTime);
        
        if (response.getStatus() >= 400) {
            logSecurityEvent("ERROR_RESPONSE", getClientIP(request), request.getRequestURI(), "HTTP_" + response.getStatus(), Map.of("status", String.valueOf(response.getStatus())));
            if (response.getStatus() == 401 || response.getStatus() == 403) trackFailedAttempt(getClientIP(request));
        }
    }

    public void recordMetric(String name, long val) {
        securityMetrics.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(val);
        lastActivity.put(name, Instant.now());
    }

    public void recordSecurityEvent(String type, long durationMs) {
        recordMetric("security_events_total", 1);
        recordMetric("security_events_" + type, 1);
        if (durationMs > MAX_RESPONSE_TIME_MS) logger.warn("🚨 Security slow event: {}ms for {}", durationMs, type);
    }

    @Async("auditTaskExecutor")
    public void logSecurityEvent(String type, String ip, String endpoint, String action, Map<String, String> details) {
        try {
            Instant now = Instant.now();
            String id = "AUDIT_" + now.toEpochMilli() + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.put("ip", ip);
            event.put("endpoint", endpoint);
            event.put("action", action);
            event.put("timestamp", com.google.cloud.Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano()));
            event.put("details", details);
            
            firestore.collection("security_audit_logs").document(id).set(event);
            recentEvents.put(id, now);
            if (recentEvents.size() > MAX_RECENT_EVENTS) cleanupOldEvents();
            
            logger.info("🔐 SECURITY AUDIT: {} | {} | {} | {}", type, ip, endpoint, action);
        } catch (Exception e) {
            logger.error("Audit log failed: {}", e.getMessage());
        }
    }

    public void logSuspiciousActivity(String userId, String reason, String ipAddress) {
        logSecurityEvent("SUSPICIOUS", ipAddress, "INTERNAL", reason, Map.of("userId", userId));
        String key = "suspicious_count_" + userId;
        try {
            var doc = firestore.collection("security_state").document(key);
            doc.set(Map.of("count", com.google.cloud.firestore.FieldValue.increment(1)), SetOptions.merge());
            if (doc.get().get().getLong("count") > 5) {
                firestore.collection("security_state").document("blacklist_" + userId).set(Map.of("blocked", true, "until", Instant.now().plus(Duration.ofHours(1)).toString()));
            }
        } catch (Exception ignored) {}
    }

    public void logTokenBlacklist(String token, String reason) {
        String hash = String.valueOf(token.hashCode());
        firestore.collection("jwt_blacklist").document(hash).set(Map.of("reason", reason, "revokedAt", Instant.now().toString()));
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            return firestore.collection("jwt_blacklist").document(String.valueOf(token.hashCode())).get().get().exists();
        } catch (Exception e) { return false; }
    }

    private boolean isSuspiciousRequest(HttpServletRequest req, String ip) {
        String ua = req.getHeader("User-Agent");
        return (ua != null && (ua.contains("bot") || ua.contains("crawler"))) || SUSPICIOUS_ENDPOINTS.stream().anyMatch(e -> req.getRequestURI().contains(e));
    }

    private boolean isBruteForceAttempt(String ip) {
        Integer count = failedAttemptCount.get(ip);
        return count != null && count >= 5 && lastFailedAttempt.get(ip).isAfter(Instant.now().minus(5, ChronoUnit.MINUTES));
    }

    private void trackFailedAttempt(String ip) {
        failedAttemptCount.put(ip, failedAttemptCount.getOrDefault(ip, 0) + 1);
        lastFailedAttempt.put(ip, Instant.now());
    }

    private void handleSuspiciousActivity(HttpServletRequest req, String ip) {
        logger.warn("🚨 SUSPICIOUS: {} at {}", ip, req.getRequestURI());
        logSecurityEvent("THREAT_DETECTED", ip, req.getRequestURI(), "SUSPICIOUS_PATTERN", Map.of("ua", req.getHeader("User-Agent")));
    }

    private void handleBruteForceAttempt(HttpServletRequest req, String ip) {
        logger.error("🚨 BRUTE_FORCE: {} at {}", ip, req.getRequestURI());
        logSecurityEvent("BRUTE_FORCE", ip, req.getRequestURI(), "BLOCKED", Map.of("reason", "Too many failures"));
    }

    private void cleanupOldEvents() {
        recentEvents.entrySet().removeIf(e -> e.getValue().isBefore(Instant.now().minus(Duration.ofHours(1))));
    }

    private String getClientIP(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    // Data classes / Inner classes for compatibility with existing code calling this
    public static class SecurityEvent {
        private final Instant timestamp;
        private final String type;
        private final String ip;
        private final String endpoint;
        private final String details;
        public SecurityEvent(Instant t, String type, String ip, String e, String d) {
            this.timestamp = t; this.type = type; this.ip = ip; this.endpoint = e; this.details = d;
        }
        public Instant getTimestamp() { return timestamp; }
        public String getEventType() { return type; }
        public String getClientIP() { return ip; }
    }
}
