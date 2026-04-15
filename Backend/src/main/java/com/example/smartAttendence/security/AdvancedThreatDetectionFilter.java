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
    
    @Autowired
    public AdvancedThreatDetectionFilter(AdvancedInputValidator advancedInputValidator, 
                                        @Nullable Firestore firestore,
                                        SecurityAuditLogger auditLogger) {
        this.advancedInputValidator = advancedInputValidator;
        this.firestore = firestore;
        this.auditLogger = auditLogger;
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
        if (isSuspiciousBehavior(request, ip)) return ThreatLevel.CRITICAL;
        if (isSuspiciousPattern(request)) return ThreatLevel.HIGH;
        
        return ThreatLevel.LOW;
    }

    private boolean isSuspiciousRate(String ip) {
        AtomicInteger count = rateCache.get(ip, k -> new AtomicInteger(0));
        return count.incrementAndGet() > 150;
    }

    private boolean isSuspiciousBehavior(HttpServletRequest request, String ip) {
        Integer fails = failureCountCache.getIfPresent(ip);
        if (fails != null && fails > 5) return true;

        if (request.getRequestURI().contains("/admin") && !isKnownAdminIP(ip)) return true;

        Instant now = Instant.now();
        Instant last = lastRequestTime.get(ip);
        if (last != null && now.minusSeconds(1).isBefore(last)) {
            int count = requestCounts.getOrDefault(ip, 0) + 1;
            requestCounts.put(ip, count);
            if (count > 15) return true;
        } else {
            requestCounts.put(ip, 1);
        }
        lastRequestTime.put(ip, now);
        return false;
    }

    private boolean isSuspiciousPattern(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null) return false;
        var result = advancedInputValidator.validateInput(query, "query");
        return !result.isValid();
    }

    private boolean isKnownAdminIP(String ip) {
        try {
            return firestore.collection("admin_ips").document(ip).get().get().exists();
        } catch (Exception e) { return false; }
    }

    private boolean isStaticOrPublicAsset(String e) {
        return e.contains("/css/") || e.contains("/js/") || e.endsWith(".png") || e.endsWith(".svg");
    }

    private String getClientIP(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private enum ThreatLevel { LOW, HIGH, CRITICAL }
}
