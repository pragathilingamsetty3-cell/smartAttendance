package com.example.smartAttendence.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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

/**
 * 🔐 PRODUCTION-GRADE CONSOLIDATED SECURITY LOGGER
 * 
 * Consolidated security audit logging, performance monitoring, and threat detection
 * Combines functionality from SecurityAuditLogger, SecurityPerformanceMonitor, 
 * and SecurityMonitoringInterceptor into a single unified component
 * Tracks all security events with detailed context for 10K+ users
 * Enhanced performance with async operations and optimized Redis usage
 */
@Component
public class SecurityAuditLogger implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditLogger.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Performance metrics (from SecurityPerformanceMonitor)
    private final Map<String, AtomicLong> securityMetrics = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();
    
    // Security event tracking (from SecurityMonitoringInterceptor)
    private final Map<String, List<SecurityEvent>> securityEvents = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastFailedAttempt = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAttemptCount = new ConcurrentHashMap<>();
    
    // Suspicious patterns (from SecurityMonitoringInterceptor)
    private static final List<String> SUSPICIOUS_ENDPOINTS = Arrays.asList(
        "/admin", "/api/v1/admin", "/api/v1/users", "/api/v1/system"
    );
    
    private static final List<String> ATTACK_INDICATORS = Arrays.asList(
        "sql", "script", "<", ">", "javascript:", "vbscript:", "onload", "onerror",
        "select", "drop", "delete", "insert", "update", "union", "--", "/*", "*/",
        "xp_", "sp_", "exec", "eval", "system", "cmd", "powershell"
    );

    // Audit log storage
    private final Map<String, Instant> recentEvents = new ConcurrentHashMap<>();
    
    // Redis keys
    private static final String AUDIT_PREFIX = "security:audit:";
    private static final String COMPLIANCE_PREFIX = "security:compliance:";
    private static final String METRICS_PREFIX = "security:metrics:";
    private static final String PERFORMANCE_PREFIX = "security:performance:";
    
    // Performance thresholds for 10K users
    private static final int MAX_RESPONSE_TIME_MS = 500; // 500ms max response time - adjusted for security operations
    private static final int MAX_CPU_USAGE_PERCENT = 80; // 80% max CPU usage
    private static final int MAX_MEMORY_USAGE_PERCENT = 85; // 85% max memory usage
    private static final int MAX_CACHE_SIZE = 100000; // 100K max cache entries
    
    // Audit retention
    private static final Duration AUDIT_RETENTION = Duration.ofDays(90);
    private static final int MAX_RECENT_EVENTS = 10000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIP = getClientIP(request);
        String endpoint = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        long startTime = System.currentTimeMillis();
        
        // Log all requests for security monitoring (from SecurityMonitoringInterceptor)
        logSecurityEvent("REQUEST", clientIP, endpoint, method, 
            Map.of("userAgent", userAgent != null ? userAgent : "unknown",
                   "timestamp", Instant.now().toString()));
        
        // Check for suspicious activities (from SecurityMonitoringInterceptor)
        if (isSuspiciousRequest(request, clientIP)) {
            handleSuspiciousActivity(request, clientIP);
        }
        
        // Check for brute force patterns (from SecurityMonitoringInterceptor)
        if (isBruteForceAttempt(clientIP, endpoint)) {
            handleBruteForceAttempt(request, clientIP);
        }
        
        // Log security-relevant requests
        if (isSecurityRelevantEndpoint(endpoint)) {
            logSecurityEvent("API_ACCESS", clientIP, endpoint, method, 
                Map.of("userAgent", userAgent != null ? userAgent : "unknown",
                       "timestamp", Instant.now().toString()));
        }
        
        // Store start time for performance tracking
        request.setAttribute("security_start_time", startTime);
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Performance tracking (from SecurityPerformanceMonitor)
        Long startTime = (Long) request.getAttribute("security_start_time");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            recordSecurityEvent("REQUEST_PROCESSING", duration);
        }
        
        String clientIP = getClientIP(request);
        String endpoint = request.getRequestURI();
        int status = response.getStatus();
        
        // Log failed requests (from SecurityMonitoringInterceptor)
        if (status >= 400) {
            logSecurityEvent("ERROR_RESPONSE", clientIP, endpoint, "HTTP_" + status,
                Map.of("statusCode", String.valueOf(status),
                       "reason", getSecurityReason(status)));
            
            // Track failed authentication attempts
            if (status == 401 || status == 403) {
                trackFailedAttempt(clientIP);
            }
        }
        
        // Log successful admin access
        if (status == 200 && isAdminEndpoint(endpoint)) {
            logSecurityEvent("ADMIN_ACCESS", clientIP, endpoint, "SUCCESS",
                Map.of("statusCode", String.valueOf(status)));
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String clientIP = getClientIP(request);
        String endpoint = request.getRequestURI();
        int statusCode = response.getStatus();
        
        // Log security events based on response
        if (statusCode == 401 || statusCode == 403) {
            logSecurityEvent("ACCESS_DENIED", clientIP, endpoint, "HTTP_" + statusCode,
                Map.of("statusCode", String.valueOf(statusCode),
                       "reason", getSecurityReason(statusCode)));
        } else if (statusCode == 429) {
            logSecurityEvent("RATE_LIMITED", clientIP, endpoint, "HTTP_429",
                Map.of("statusCode", String.valueOf(statusCode),
                       "reason", "Rate limit exceeded"));
        } else if (statusCode >= 400) {
            logSecurityEvent("CLIENT_ERROR", clientIP, endpoint, "HTTP_" + statusCode,
                Map.of("statusCode", String.valueOf(statusCode)));
        }
        
        // Log exceptions (from SecurityMonitoringInterceptor)
        if (ex != null) {
            logSecurityEvent("EXCEPTION", clientIP, endpoint, "EXCEPTION",
                Map.of("exception", ex.getMessage(),
                       "exceptionType", ex.getClass().getSimpleName()));
        }
        
        // Performance tracking completion
        Long startTime = (Long) request.getAttribute("security_start_time");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            recordSecurityEvent("REQUEST_COMPLETE", duration);
        }
    }

    /**
     * 🔐 RECORD SECURITY METRIC (from SecurityPerformanceMonitor)
     */
    public void recordMetric(String metricName, long value) {
        securityMetrics.computeIfAbsent(metricName, k -> new AtomicLong(0)).addAndGet(value);
        lastActivity.put(metricName, Instant.now());
        
        // Store in Redis for persistence
        try {
            String key = METRICS_PREFIX + metricName;
            redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofMinutes(5));
        } catch (Exception e) {
            // Metrics storage failure is non-critical
        }
    }

    /**
     * 🔐 RECORD SECURITY EVENT WITH TIMING (from SecurityPerformanceMonitor)
     */
    public void recordSecurityEvent(String eventType, long durationMs) {
        recordMetric("security_events_total", 1);
        recordMetric("security_events_" + eventType, 1);
        recordMetric("security_duration_" + eventType, durationMs);
        
        // Check performance thresholds
        if (durationMs > MAX_RESPONSE_TIME_MS) {
            logger.warn("🚨 Security event took too long: {}ms for event: {}", durationMs, eventType);
            recordMetric("security_slow_events", 1);
        }
    }

    /**
     * 🔐 LOG SECURITY EVENT
     */
    public void logSecurityEvent(String eventType, String clientIP, String endpoint, String action, Map<String, String> details) {
        try {
            Instant timestamp = Instant.now();
            String eventId = generateEventId();
            
            // Create audit entry
            SecurityAuditEvent event = new SecurityAuditEvent();
            event.setEventId(eventId);
            event.setEventType(eventType);
            event.setClientIP(clientIP);
            event.setEndpoint(endpoint);
            event.setAction(action);
            event.setTimestamp(timestamp);
            event.setDetails(details);
            
            // Store in Redis
            String key = AUDIT_PREFIX + eventId;
            String auditData = serializeAuditEvent(event);
            redisTemplate.opsForValue().set(key, auditData, AUDIT_RETENTION);
            
            // Store in recent events (memory)
            recentEvents.put(eventId, timestamp);
            if (recentEvents.size() > MAX_RECENT_EVENTS) {
                cleanupOldEvents();
            }
            
            // Log to file
            logger.info("🔐 SECURITY AUDIT: {} | {} | {} | {} | {}", 
                eventType, clientIP, endpoint, action, details);
            
            // Update compliance metrics
            updateComplianceMetrics(eventType);
            
            // Record performance (integrated from SecurityPerformanceMonitor)
            recordSecurityEvent(eventType, 0);
            
        } catch (Exception e) {
            logger.error("Failed to log security event: {}", e.getMessage());
        }
    }

    /**
     * 🔐 LOG AUTHENTICATION EVENT
     */
    public void logAuthenticationEvent(String eventType, String clientIP, String userId, boolean success, String reason) {
        Map<String, String> details = Map.of(
            "userId", userId != null ? userId : "anonymous",
            "success", String.valueOf(success),
            "reason", reason != null ? reason : "N/A"
        );
        
        logSecurityEvent("AUTH_" + eventType, clientIP, "/auth", eventType, details);
    }

    /**
     * 🔐 LOG AUTHORIZATION EVENT
     */
    public void logAuthorizationEvent(String clientIP, String userId, String endpoint, String role, boolean authorized) {
        Map<String, String> details = Map.of(
            "userId", userId != null ? userId : "anonymous",
            "role", role != null ? role : "unknown",
            "authorized", String.valueOf(authorized)
        );
        
        logSecurityEvent("AUTHZ_CHECK", clientIP, endpoint, authorized ? "GRANTED" : "DENIED", details);
    }

    /**
     * 🔐 LOG THREAT DETECTION EVENT
     */
    public void logThreatDetectionEvent(String threatType, String clientIP, String endpoint, String severity, boolean blocked) {
        Map<String, String> details = Map.of(
            "threatType", threatType,
            "severity", severity,
            "blocked", String.valueOf(blocked)
        );
        
        logSecurityEvent("THREAT_" + threatType.toUpperCase(), clientIP, endpoint, 
            blocked ? "BLOCKED" : "DETECTED", details);
    }

    /**
     * 🔐 LOG DATA ACCESS EVENT
     */
    public void logDataAccessEvent(String clientIP, String userId, String resource, String action, boolean authorized) {
        Map<String, String> details = Map.of(
            "userId", userId != null ? userId : "anonymous",
            "resource", resource,
            "action", action,
            "authorized", String.valueOf(authorized)
        );
        
        logSecurityEvent("DATA_ACCESS", clientIP, resource, action, details);
    }

    /**
     * 🔐 LOG CONFIGURATION CHANGE
     */
    public void logConfigurationChange(String userId, String component, String setting, String oldValue, String newValue) {
        Map<String, String> details = Map.of(
            "userId", userId,
            "component", component,
            "setting", setting,
            "oldValue", oldValue,
            "newValue", newValue
        );
        
        logSecurityEvent("CONFIG_CHANGE", userId, component, "MODIFY", details);
    }

    /**
     * 🔐 GET AUDIT STATISTICS
     */
    public AuditStatistics getAuditStatistics() {
        AuditStatistics stats = new AuditStatistics();
        
        try {
            // Count events by type
            stats.setTotalEvents(countRedisKeys(AUDIT_PREFIX + "*"));
            stats.setAuthenticationEvents(countEventsByType("AUTH_"));
            stats.setAuthorizationEvents(countEventsByType("AUTHZ_"));
            stats.setThreatEvents(countEventsByType("THREAT_"));
            stats.setDataAccessEvents(countEventsByType("DATA_ACCESS"));
            stats.setConfigurationEvents(countEventsByType("CONFIG_CHANGE"));
            
            // Recent activity
            stats.setRecentEvents(recentEvents.size());
            stats.setEventsLastHour(getEventsInLastHour());
            
            // Compliance metrics
            stats.setComplianceScore(calculateComplianceScore());
            
        } catch (Exception e) {
            logger.error("Failed to generate audit statistics: {}", e.getMessage());
        }
        
        return stats;
    }

    /**
     * 🔐 GET SECURITY PERFORMANCE SUMMARY (from SecurityPerformanceMonitor)
     */
    public SecurityPerformanceSummary getPerformanceSummary() {
        SecurityPerformanceSummary summary = new SecurityPerformanceSummary();
        
        try {
            // Collect metrics from memory
            summary.setTotalSecurityEvents(getMetricValue("security_events_total"));
            summary.setAverageResponseTime(getAverageResponseTime());
            summary.setCacheHitRate(getCacheHitRate());
            summary.setRateLimitEfficiency(getRateLimitEfficiency());
            summary.setThreatDetectionRate(getThreatDetectionRate());
            
            // Collect system health metrics
            summary.setCpuUsage(getCpuUsage());
            summary.setMemoryUsage(getMemoryUsage());
            summary.setCacheSize(getCacheSize());
            
            // Calculate performance score
            summary.setPerformanceScore(calculatePerformanceScore(summary));
            
        } catch (Exception e) {
            logger.error("Failed to generate performance summary: {}", e.getMessage());
            // Return safe defaults
            summary.setPerformanceScore(50); // Neutral score
        }
        
        return summary;
    }

    /**
     * 🔐 GET REAL-TIME SECURITY HEALTH (from SecurityPerformanceMonitor)
     */
    public SecurityHealthStatus getSecurityHealthStatus() {
        SecurityHealthStatus status = new SecurityHealthStatus();
        
        try {
            // Check individual components
            status.setRateLimitHealth(checkRateLimitHealth());
            status.setThreatDetectionHealth(checkThreatDetectionHealth());
            status.setValidationHealth(checkValidationHealth());
            status.setCacheHealth(checkCacheHealth());
            
            // Overall health
            status.setOverallHealth(calculateOverallHealth(status));
            
        } catch (Exception e) {
            logger.error("Failed to check security health: {}", e.getMessage());
            status.setOverallHealth("DEGRADED");
        }
        
        return status;
    }

    // 🔐 CONSOLIDATED SECURITY LOGGING METHODS
    
    /**
     * 🔐 LOG SECURITY EVENT (Enhanced from SecurityEventLogger)
     */
    @Async
    public CompletableFuture<Void> logSecurityEventAsync(String eventType, String userId, String details) {
        return CompletableFuture.runAsync(() -> logSecurityEvent(eventType, userId, details));
    }
    
    /**
     * 🔐 LOG SECURITY EVENT (Synchronous version)
     */
    public void logSecurityEvent(String eventType, String userId, String details) {
        try {
            Instant timestamp = Instant.now();
            String eventId = generateEventId();
            
            // Create consolidated audit entry
            SecurityAuditEvent event = new SecurityAuditEvent();
            event.setEventId(eventId);
            event.setEventType(eventType);
            event.setClientIP(userId.equals("system") ? "SYSTEM" : "USER");
            event.setEndpoint("INTERNAL");
            event.setAction(eventType);
            event.setTimestamp(timestamp);
            event.setDetails(Map.of(
                "userId", userId,
                "details", details,
                "source", "SecurityEventLogger_Consolidated"
            ));
            
            // Store in Redis with optimized key structure
            String key = AUDIT_PREFIX + eventId;
            String auditData = serializeAuditEvent(event);
            redisTemplate.opsForValue().set(key, auditData, AUDIT_RETENTION);
            
            // Store in recent events (memory)
            recentEvents.put(eventId, timestamp);
            if (recentEvents.size() > MAX_RECENT_EVENTS) {
                cleanupOldEvents();
            }
            
            // Log to file
            logger.info("🔐 SECURITY AUDIT: {} | {} | {} | {} | {}", 
                eventType, userId, "INTERNAL", eventType, details);
            
            // Update compliance metrics
            updateComplianceMetrics(eventType);
            
            // Record performance (integrated from SecurityPerformanceMonitor)
            recordSecurityEvent(eventType, 0);
            
        } catch (Exception e) {
            logger.error("Failed to log security event: {}", e.getMessage());
        }
    }
    
    /**
     * 🔐 LOG SUSPICIOUS ACTIVITY (Enhanced from SecurityEventLogger)
     */
    @Async
    public CompletableFuture<Void> logSuspiciousActivityAsync(String userId, String reason, String ipAddress) {
        return CompletableFuture.runAsync(() -> logSuspiciousActivity(userId, reason, ipAddress));
    }
    
    public void logSuspiciousActivity(String userId, String reason, String ipAddress) {
        String details = String.format("Suspicious activity detected: %s from IP %s", reason, ipAddress);
        logSecurityEvent("SUSPICIOUS_ACTIVITY", userId, details);
        
        // 🔐 BLACKLIST USER IF TOO MANY SUSPICIOUS ACTIVITIES
        String suspiciousKey = "suspicious_count:" + userId;
        try {
            Long count = redisTemplate.opsForValue().increment(suspiciousKey);
            redisTemplate.expire(suspiciousKey, Duration.ofHours(24));
            
            if (count > 5) {
                String blacklistKey = "blacklisted_user:" + userId;
                redisTemplate.opsForValue().set(blacklistKey, "true", Duration.ofHours(1));
                logSecurityEvent("USER_BLACKLISTED", userId, "Auto-blacklisted due to suspicious activities");
            }
        } catch (Exception e) {
            logger.error("Failed to check suspicious activity count: {}", e.getMessage());
        }
    }
    
    /**
     * 🔐 CHECK USER BLACKLIST STATUS (Enhanced from SecurityEventLogger)
     */
    public boolean isUserBlacklisted(String userId) {
        try {
            String blacklistKey = "blacklisted_user:" + userId;
            return redisTemplate.hasKey(blacklistKey);
        } catch (Exception e) {
            logger.error("Failed to check user blacklist status: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 🔐 LOG TOKEN BLACKLIST (Enhanced from SecurityEventLogger)
     */
    @Async
    public CompletableFuture<Void> logTokenBlacklistAsync(String token, String reason) {
        return CompletableFuture.runAsync(() -> logTokenBlacklist(token, reason));
    }
    
    public void logTokenBlacklist(String token, String reason) {
        try {
            // Use hash-based token identification for privacy
            String tokenHash = UUID.nameUUIDFromBytes(token.getBytes()).toString();
            String blacklistKey = "blacklisted_token:" + tokenHash;
            redisTemplate.opsForValue().set(blacklistKey, reason, Duration.ofDays(14)); // Extended from 7 days
            logSecurityEvent("TOKEN_BLACKLISTED", "system", reason);
        } catch (Exception e) {
            logger.error("Failed to blacklist token: {}", e.getMessage());
        }
    }
    
    /**
     * 🔐 CHECK TOKEN BLACKLIST STATUS (Enhanced from SecurityEventLogger)
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            // Use hash-based token identification for privacy
            String tokenHash = UUID.nameUUIDFromBytes(token.getBytes()).toString();
            String blacklistKey = "blacklisted_token:" + tokenHash;
            return redisTemplate.hasKey(blacklistKey);
        } catch (Exception e) {
            logger.error("Failed to check token blacklist status: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 🔐 BATCH SECURITY EVENTS (Performance Enhancement)
     */
    @Async
    public CompletableFuture<Void> logBatchSecurityEvents(List<SecurityEventBatch> events) {
        return CompletableFuture.runAsync(() -> {
            for (SecurityEventBatch event : events) {
                logSecurityEvent(event.eventType, event.userId, event.details);
            }
        });
    }

    // 🔐 SECURITY MONITORING METHODS (from SecurityMonitoringInterceptor)
    
    /**
     * Check if request is suspicious (from SecurityMonitoringInterceptor)
     */
    private boolean isSuspiciousRequest(HttpServletRequest request, String clientIP) {
        String endpoint = request.getRequestURI();
        String queryString = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");
        
        // Check for suspicious endpoints
        for (String suspicious : SUSPICIOUS_ENDPOINTS) {
            if (endpoint.toLowerCase().contains(suspicious.toLowerCase()) && 
                !isKnownAdminIP(clientIP)) {
                return true;
            }
        }
        
        // Check query string for attack patterns
        if (queryString != null) {
            String lowerQuery = queryString.toLowerCase();
            for (String indicator : ATTACK_INDICATORS) {
                if (lowerQuery.contains(indicator)) {
                    return true;
                }
            }
        }
        
        // Check for missing or suspicious user agent
        if (userAgent == null || userAgent.isEmpty() || 
            userAgent.toLowerCase().contains("bot") || 
            userAgent.toLowerCase().contains("crawler")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check for brute force attempts (from SecurityMonitoringInterceptor)
     */
    private boolean isBruteForceAttempt(String clientIP, String endpoint) {
        // Check multiple failed attempts in short time
        Integer failedCount = failedAttemptCount.get(clientIP);
        Instant lastFailed = lastFailedAttempt.get(clientIP);
        
        if (failedCount != null && failedCount >= 5 && lastFailed != null) {
            // If 5+ failed attempts within 5 minutes
            if (lastFailed.isAfter(Instant.now().minus(5, ChronoUnit.MINUTES))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handle suspicious activity (from SecurityMonitoringInterceptor)
     */
    private void handleSuspiciousActivity(HttpServletRequest request, String clientIP) {
        String endpoint = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");
        
        logger.warn("🚨 SECURITY ALERT: Suspicious activity detected from IP: {} to endpoint: {}", clientIP, endpoint);
        
        // Log detailed security event
        logSecurityEvent("SUSPICIOUS_ACTIVITY", clientIP, endpoint, "User-Agent: " + userAgent, 
            Map.of("userAgent", userAgent != null ? userAgent : "unknown",
                   "threatType", "SUSPICIOUS_PATTERN"));
        
        // Add to security events list
        SecurityEvent event = new SecurityEvent(
            Instant.now(),
            "SUSPICIOUS_ACTIVITY",
            clientIP,
            endpoint,
            "Suspicious request pattern detected"
        );
        
        securityEvents.computeIfAbsent(clientIP, k -> new ArrayList<>()).add(event);
        
        // Trigger additional monitoring
        triggerSecurityAlert(event);
    }
    
    /**
     * Handle brute force attempt (from SecurityMonitoringInterceptor)
     */
    private void handleBruteForceAttempt(HttpServletRequest request, String clientIP) {
        String endpoint = request.getRequestURI();
        
        logger.error("🚨 SECURITY ALERT: Brute force attack detected from IP: {} on endpoint: {}", clientIP, endpoint);
        
        // Log detailed security event
        logSecurityEvent("BRUTE_FORCE", clientIP, endpoint, "Multiple failed attempts detected",
            Map.of("threatType", "BRUTE_FORCE",
                   "severity", "HIGH"));
        
        // Add to security events list
        SecurityEvent event = new SecurityEvent(
            Instant.now(),
            "BRUTE_FORCE",
            clientIP,
            endpoint,
            "Brute force attack detected"
        );
        
        securityEvents.computeIfAbsent(clientIP, k -> new ArrayList<>()).add(event);
        
        // Trigger immediate security alert
        triggerSecurityAlert(event);
        
        // Consider implementing IP blocking here
        // blockIP(clientIP, Duration.ofHours(1));
    }
    
    /**
     * Track failed authentication attempts (from SecurityMonitoringInterceptor)
     */
    private void trackFailedAttempt(String clientIP) {
        int count = failedAttemptCount.getOrDefault(clientIP, 0) + 1;
        failedAttemptCount.put(clientIP, count);
        lastFailedAttempt.put(clientIP, Instant.now());
        
        // Reset count after 15 minutes of no attempts
        if (count == 1) {
            // Schedule reset (simplified - in production use proper scheduler)
            resetFailedAttemptsAfterDelay(clientIP);
        }
    }
    
    /**
     * Reset failed attempts after delay (from SecurityMonitoringInterceptor)
     */
    private void resetFailedAttemptsAfterDelay(String clientIP) {
        // In production, use proper scheduling (e.g., @Scheduled)
        // This is a simplified implementation
        new Thread(() -> {
            try {
                Thread.sleep(15 * 60 * 1000); // 15 minutes
                failedAttemptCount.remove(clientIP);
                lastFailedAttempt.remove(clientIP);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Get security events for analysis (from SecurityMonitoringInterceptor)
     */
    public List<SecurityEvent> getSecurityEvents(String clientIP) {
        return securityEvents.getOrDefault(clientIP, new ArrayList<>());
    }
    
    /**
     * Get all security events (from SecurityMonitoringInterceptor)
     */
    public Map<String, List<SecurityEvent>> getAllSecurityEvents() {
        return new HashMap<>(securityEvents);
    }
    
    /**
     * Clear old security events (from SecurityMonitoringInterceptor)
     */
    public void clearOldEvents() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        
        securityEvents.values().forEach(events -> {
            events.removeIf(event -> event.getTimestamp().isBefore(cutoff));
        });
        
        // Remove empty event lists
        securityEvents.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Clean up old metrics
        cleanupOldMetrics();
    }
    
    /**
     * Trigger security alert (from SecurityMonitoringInterceptor)
     */
    private void triggerSecurityAlert(SecurityEvent event) {
        // In production, integrate with:
        // - SIEM systems
        // - Email alerts
        // - Slack notifications
        // - SMS alerts
        // - Security operations center
        
        logger.error("🚨 SECURITY ALERT TRIGGERED: {} from {} at {}", 
            event.getEventType(), event.getClientIP(), event.getTimestamp());
    }

    // 🔐 PRIVATE HELPER METHODS
    
    private boolean isSecurityRelevantEndpoint(String endpoint) {
        return endpoint.startsWith("/auth/") || 
               endpoint.startsWith("/admin/") || 
               endpoint.startsWith("/api/v1/admin/") ||
               endpoint.startsWith("/api/v1/faculty/") ||
               endpoint.startsWith("/api/v1/student/") ||
               endpoint.contains("/attendance/") ||
               endpoint.startsWith("/actuator/");
    }

    private boolean isAdminEndpoint(String endpoint) {
        return endpoint.contains("/admin") || endpoint.contains("/api/v1/admin");
    }
    
    private boolean isKnownAdminIP(String clientIP) {
        // In production, maintain a whitelist of admin IPs
        // This is a simplified implementation
        return "127.0.0.1".equals(clientIP) || "::1".equals(clientIP) || "0:0:0:0:0:0:0:1".equals(clientIP);
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

    private String generateEventId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    private String serializeAuditEvent(SecurityAuditEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(event.getEventType()).append("|");
        sb.append(event.getClientIP()).append("|");
        sb.append(event.getEndpoint()).append("|");
        sb.append(event.getAction()).append("|");
        sb.append(event.getTimestamp().toString()).append("|");
        sb.append(event.getDetails().toString());
        return sb.toString();
    }

    private String getSecurityReason(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 429 -> "Rate Limited";
            case 500 -> "Internal Server Error";
            default -> "Security Violation";
        };
    }

    private void updateComplianceMetrics(String eventType) {
        try {
            String key = COMPLIANCE_PREFIX + eventType;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofDays(30));
        } catch (Exception e) {
            // Compliance metrics update failure is non-critical
        }
    }

    private void cleanupOldEvents() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        recentEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }

    private void cleanupOldMetrics() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        
        // Clean up in-memory metrics
        lastActivity.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        // Clean up Redis metrics (handled by TTL automatically)
    }

    private long countRedisKeys(String pattern) {
        try {
            return redisTemplate.keys(pattern).stream().count();
        } catch (Exception e) {
            return 0;
        }
    }

    private long countEventsByType(String type) {
        return countRedisKeys(AUDIT_PREFIX + type + "*");
    }

    private long getEventsInLastHour() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        return recentEvents.values().stream()
            .filter(timestamp -> timestamp.isAfter(cutoff))
            .count();
    }

    private double calculateComplianceScore() {
        try {
            long totalEvents = countRedisKeys(AUDIT_PREFIX + "*");
            if (totalEvents == 0) return 100.0;
            
            // Check for required event types
            long authEvents = countEventsByType("AUTH_");
            long threatEvents = countEventsByType("THREAT_");
            long dataEvents = countEventsByType("DATA_ACCESS");
            
            // Score based on coverage
            double score = 0.0;
            if (authEvents > 0) score += 33.3;
            if (threatEvents > 0) score += 33.3;
            if (dataEvents > 0) score += 33.4;
            
            return score;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // 🔐 PERFORMANCE MONITORING HELPER METHODS (from SecurityPerformanceMonitor)

    private long getMetricValue(String metricName) {
        AtomicLong value = securityMetrics.get(metricName);
        return value != null ? value.get() : 0;
    }

    private double getAverageResponseTime() {
        long totalEvents = getMetricValue("security_events_total");
        long totalDuration = getMetricValue("security_duration_total");
        
        return totalEvents > 0 ? (double) totalDuration / totalEvents : 0.0;
    }

    private double getCacheHitRate() {
        long hits = getMetricValue("cache_validation_hit") + 
                   getMetricValue("cache_rate_limit_hit") + 
                   getMetricValue("cache_threat_hit");
        long misses = getMetricValue("cache_validation_miss") + 
                     getMetricValue("cache_rate_limit_miss") + 
                     getMetricValue("cache_threat_miss");
        
        long total = hits + misses;
        return total > 0 ? (double) hits / total * 100 : 0.0;
    }

    private double getRateLimitEfficiency() {
        long allowed = getMetricValue("rate_limit_allowed");
        long limited = getMetricValue("rate_limit_limited");
        
        long total = allowed + limited;
        return total > 0 ? (double) limited / total * 100 : 0.0;
    }

    private double getThreatDetectionRate() {
        long detected = getMetricValue("threat_detected");
        long clean = getMetricValue("threat_clean");
        
        long total = detected + clean;
        return total > 0 ? (double) detected / total * 100 : 0.0;
    }

    private double getCpuUsage() {
        try {
            // Simplified CPU usage calculation
            Runtime runtime = Runtime.getRuntime();
            return (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            return (double) usedMemory / runtime.maxMemory() * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int getCacheSize() {
        try {
            return securityMetrics.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int calculatePerformanceScore(SecurityPerformanceSummary summary) {
        int score = 100;
        
        // Response time penalty
        if (summary.getAverageResponseTime() > MAX_RESPONSE_TIME_MS) {
            score -= 20;
        }
        
        // CPU usage penalty
        if (summary.getCpuUsage() > MAX_CPU_USAGE_PERCENT) {
            score -= 15;
        }
        
        // Memory usage penalty
        if (summary.getMemoryUsage() > MAX_MEMORY_USAGE_PERCENT) {
            score -= 15;
        }
        
        // Cache efficiency bonus
        if (summary.getCacheHitRate() > 80) {
            score += 10;
        }
        
        return Math.max(0, Math.min(100, score));
    }

    private String checkRateLimitHealth() {
        long slowEvents = getMetricValue("security_slow_events");
        return slowEvents > 100 ? "DEGRADED" : "HEALTHY";
    }

    private String checkThreatDetectionHealth() {
        long avgDuration = getMetricValue("threat_detection_duration") / 
                          Math.max(1, getMetricValue("threat_detected") + getMetricValue("threat_clean"));
        return avgDuration > 50 ? "DEGRADED" : "HEALTHY";
    }

    private String checkValidationHealth() {
        long validationErrors = getMetricValue("validation_errors");
        return validationErrors > 50 ? "DEGRADED" : "HEALTHY";
    }

    private String checkCacheHealth() {
        double hitRate = getCacheHitRate();
        return hitRate < 70 ? "DEGRADED" : "HEALTHY";
    }

    private String calculateOverallHealth(SecurityHealthStatus status) {
        int healthy = 0;
        if ("HEALTHY".equals(status.getRateLimitHealth())) healthy++;
        if ("HEALTHY".equals(status.getThreatDetectionHealth())) healthy++;
        if ("HEALTHY".equals(status.getValidationHealth())) healthy++;
        if ("HEALTHY".equals(status.getCacheHealth())) healthy++;
        
        if (healthy == 4) return "HEALTHY";
        if (healthy >= 2) return "DEGRADED";
        return "CRITICAL";
    }

    // 🔐 DATA CLASSES - MOVED TO SEPARATE FILES FOR BETTER ARCHITECTURE
    // SecurityAuditEvent -> SecurityAuditEvent.java
    // SecurityEvent -> SecurityEvent.java
    // AuditStatistics, SecurityPerformanceSummary, SecurityHealthStatus below
    
    public static class AuditStatistics {
        private long totalEvents;
        private long authenticationEvents;
        private long authorizationEvents;
        private long threatEvents;
        private long dataAccessEvents;
        private long configurationEvents;
        private long recentEvents;
        private long eventsLastHour;
        private double complianceScore;

        // Getters and setters
        public long getTotalEvents() { return totalEvents; }
        public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }
        public long getAuthenticationEvents() { return authenticationEvents; }
        public void setAuthenticationEvents(long authenticationEvents) { this.authenticationEvents = authenticationEvents; }
        public long getAuthorizationEvents() { return authorizationEvents; }
        public void setAuthorizationEvents(long authorizationEvents) { this.authorizationEvents = authorizationEvents; }
        public long getThreatEvents() { return threatEvents; }
        public void setThreatEvents(long threatEvents) { this.threatEvents = threatEvents; }
        public long getDataAccessEvents() { return dataAccessEvents; }
        public void setDataAccessEvents(long dataAccessEvents) { this.dataAccessEvents = dataAccessEvents; }
        public long getConfigurationEvents() { return configurationEvents; }
        public void setConfigurationEvents(long configurationEvents) { this.configurationEvents = configurationEvents; }
        public long getRecentEvents() { return recentEvents; }
        public void setRecentEvents(long recentEvents) { this.recentEvents = recentEvents; }
        public long getEventsLastHour() { return eventsLastHour; }
        public void setEventsLastHour(long eventsLastHour) { this.eventsLastHour = eventsLastHour; }
        public double getComplianceScore() { return complianceScore; }
        public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }
    }

    public static class SecurityPerformanceSummary {
        private long totalSecurityEvents;
        private double averageResponseTime;
        private double cacheHitRate;
        private double rateLimitEfficiency;
        private double threatDetectionRate;
        private double cpuUsage;
        private double memoryUsage;
        private int cacheSize;
        private int performanceScore;

        // Getters and setters
        public long getTotalSecurityEvents() { return totalSecurityEvents; }
        public void setTotalSecurityEvents(long totalSecurityEvents) { this.totalSecurityEvents = totalSecurityEvents; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
        public double getRateLimitEfficiency() { return rateLimitEfficiency; }
        public void setRateLimitEfficiency(double rateLimitEfficiency) { this.rateLimitEfficiency = rateLimitEfficiency; }
        public double getThreatDetectionRate() { return threatDetectionRate; }
        public void setThreatDetectionRate(double threatDetectionRate) { this.threatDetectionRate = threatDetectionRate; }
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
        public int getCacheSize() { return cacheSize; }
        public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
        public int getPerformanceScore() { return performanceScore; }
        public void setPerformanceScore(int performanceScore) { this.performanceScore = performanceScore; }
    }

    public static class SecurityHealthStatus {
        private String rateLimitHealth;
        private String threatDetectionHealth;
        private String validationHealth;
        private String cacheHealth;
        private String overallHealth;

        // Getters and setters
        public String getRateLimitHealth() { return rateLimitHealth; }
        public void setRateLimitHealth(String rateLimitHealth) { this.rateLimitHealth = rateLimitHealth; }
        public String getThreatDetectionHealth() { return threatDetectionHealth; }
        public void setThreatDetectionHealth(String threatDetectionHealth) { this.threatDetectionHealth = threatDetectionHealth; }
        public String getValidationHealth() { return validationHealth; }
        public void setValidationHealth(String validationHealth) { this.validationHealth = validationHealth; }
        public String getCacheHealth() { return cacheHealth; }
        public void setCacheHealth(String cacheHealth) { this.cacheHealth = cacheHealth; }
        public String getOverallHealth() { return overallHealth; }
        public void setOverallHealth(String overallHealth) { this.overallHealth = overallHealth; }
    }

    public static class SecurityEventBatch {
        public String eventType;
        public String userId;
        public String details;
        
        public SecurityEventBatch(String eventType, String userId, String details) {
            this.eventType = eventType;
            this.userId = userId;
            this.details = details;
        }
    }
}
