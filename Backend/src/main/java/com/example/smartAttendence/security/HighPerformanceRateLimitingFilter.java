package com.example.smartAttendence.security;

import jakarta.annotation.PostConstruct;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HighPerformanceRateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HighPerformanceRateLimitingFilter.class);

    // 🚀 LOCAL HIGH-SPEED CACHE (Caffeine)
    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Value("${rate-limit.global.requests-per-minute:1000}") 
    private int globalRequestsPerMinute;

    @Value("${rate-limit.auth.requests-per-minute:100}") 
    private int authRequestsPerMinute;

    @Value("${rate-limit.admin.requests-per-minute:500}") 
    private int adminRequestsPerMinute;

    @Value("${rate-limit.heartbeat.requests-per-minute:2000}") 
    private int heartbeatRequestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String endpoint = request.getRequestURI();
        try {
            String clientIP = getClientIP(request);
            
            // Bypass for localhost and health checks
            if (isBypassEndpoint(endpoint, clientIP, request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String clientId = getClientIdentifier(request);
            int maxRequests = getMaxRequestsForEndpoint(endpoint);

            if (isRateLimited(clientId, endpoint, maxRequests)) {
                logger.warn("🚫 Rate limit exceeded for client {} on endpoint {}", clientId, endpoint);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":60}");
                return;
            }
        
        } catch (Throwable t) {
            logger.error("🚨 RateLimitFilter error: {}", t.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isBypassEndpoint(String endpoint, String clientIP, String method) {
        return "127.0.0.1".equals(clientIP) || "::1".equals(clientIP) ||
               endpoint.startsWith("/actuator/") || 
               "OPTIONS".equalsIgnoreCase(method);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String ip = getClientIP(request);
        String endpoint = request.getRequestURI();
        
        if (endpoint.contains("/heartbeat")) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return ip + ":" + authHeader.substring(7).hashCode();
            }
        }
        return ip;
    }

    private boolean isRateLimited(String clientId, String endpoint, int maxRequests) {
        String key = endpoint + ":" + clientId;
        AtomicInteger count = requestCounts.get(key, k -> new AtomicInteger(0));
        return count.incrementAndGet() > maxRequests;
    }
    
    private int getMaxRequestsForEndpoint(String endpoint) {
        if (endpoint.contains("/auth/login")) return authRequestsPerMinute;
        if (endpoint.contains("/admin/")) return adminRequestsPerMinute;
        if (endpoint.contains("/heartbeat")) return heartbeatRequestsPerMinute;
        return globalRequestsPerMinute;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
