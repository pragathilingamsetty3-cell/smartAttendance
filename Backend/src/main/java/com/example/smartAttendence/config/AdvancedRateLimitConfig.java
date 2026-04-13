package com.example.smartAttendence.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class AdvancedRateLimitConfig {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedRateLimitConfig.class);
    
    // Intelligent retry tracking
    private final ConcurrentHashMap<String, AtomicInteger> retryCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastRetryTime = new ConcurrentHashMap<>();
    
    // Production rate limits for 50K+ users
    @Value("${rate-limit.global.requests-per-minute:5000}")
    private int globalRequestsPerMinute;

    @Value("${rate-limit.auth.requests-per-minute:200}")
    private int authRequestsPerMinute;

    @Value("${rate-limit.admin.requests-per-minute:2000}")
    private int adminRequestsPerMinute;

    @Value("${rate-limit.heartbeat.requests-per-minute:10000}")
    private int heartbeatRequestsPerMinute;
    
    @Value("${rate-limit.api.requests-per-minute:3000}")
    private int apiRequestsPerMinute;
    
    @Value("${rate-limit.upload.requests-per-minute:100}")
    private int uploadRequestsPerMinute;
    
    // Retry configuration
    @Value("${retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${retry.wait-duration-ms:1000}")
    private long retryWaitDurationMs;
    
    @Value("${retry.exponential-backoff:true}")
    private boolean exponentialBackoff;
    
    // Circuit breaker configuration
    @Value("${circuit-breaker.failure-rate-threshold:50}")
    private float circuitBreakerFailureThreshold;
    
    @Value("${circuit-breaker.wait-duration-open-ms:30000}")
    private long circuitBreakerWaitDurationOpenMs;
    
    @Value("${circuit-breaker.sliding-window-size:100}")
    private int circuitBreakerSlidingWindowSize;

    // 🔐 SAFE MODE - Apply safe defaults if values are 0 or invalid
    private int getSafeRateLimit(int configuredValue, int safeDefault) {
        return configuredValue <= 0 ? safeDefault : configuredValue;
    }
    
    /**
     * Intelligent retry logic with exponential backoff and rate limiting
     */
    private RetryConfig createIntelligentRetryConfig(String serviceName) {
        return RetryConfig.custom()
                .maxAttempts(maxRetryAttempts)
                .waitDuration(Duration.ofMillis(calculateIntelligentWaitTime(serviceName)))
                .retryOnException(ex -> shouldRetry(ex, serviceName))
                .retryOnResult(result -> shouldRetry(result, serviceName))
                .build();
    }
    
    /**
     * Calculate intelligent wait time based on service and retry history
     */
    private long calculateIntelligentWaitTime(String serviceName) {
        AtomicInteger retryCount = retryCounters.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
        long currentCount = retryCount.get();
        
        if (exponentialBackoff) {
            // Exponential backoff: 1s, 2s, 4s, 8s...
            return Math.min(retryWaitDurationMs * (1L << currentCount), 10000L); // Max 10s
        } else {
            // Linear backoff
            return Math.min(retryWaitDurationMs * (currentCount + 1), 5000L); // Max 5s
        }
    }
    
    /**
     * Determine if exception should be retried
     */
    private boolean shouldRetry(Exception ex, String serviceName) {
        // Don't retry on client errors (4xx)
        if (ex.getMessage() != null && ex.getMessage().contains("4")) {
            return false;
        }
        
        // Don't retry on authentication errors
        if (ex.getMessage() != null && 
            (ex.getMessage().contains("401") || ex.getMessage().contains("403"))) {
            return false;
        }
        
        // Check retry rate limiting
        long currentTime = System.currentTimeMillis();
        AtomicLong lastRetry = lastRetryTime.computeIfAbsent(serviceName, k -> new AtomicLong(0));
        
        if (currentTime - lastRetry.get() < 1000) { // Max 1 retry per second per service
            return false;
        }
        
        lastRetry.set(currentTime);
        return true;
    }
    
    /**
     * Determine if result should be retried
     */
    private boolean shouldRetry(Object result, String serviceName) {
        // Implement custom logic based on result
        return false; // Default: don't retry on successful results
    }

    @Bean
    @Primary
    public RateLimiterRegistry rateLimiterRegistry() {
        // 🔐 SAFE MODE - Apply safe defaults for rate limits
        int safeGlobalRpm = getSafeRateLimit(globalRequestsPerMinute, 5000);
        int safeAuthRpm = getSafeRateLimit(authRequestsPerMinute, 200);
        int safeAdminRpm = getSafeRateLimit(adminRequestsPerMinute, 2000);
        int safeHeartbeatRpm = getSafeRateLimit(heartbeatRequestsPerMinute, 10000);
        int safeApiRpm = getSafeRateLimit(apiRequestsPerMinute, 3000);
        int safeUploadRpm = getSafeRateLimit(uploadRequestsPerMinute, 100);
        
        RateLimiterConfig globalConfig = RateLimiterConfig.custom()
                .limitForPeriod(safeGlobalRpm)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterConfig authConfig = RateLimiterConfig.custom()
                .limitForPeriod(safeAuthRpm)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(3))
                .build();

        RateLimiterConfig adminConfig = RateLimiterConfig.custom()
                .limitForPeriod(safeAdminRpm)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(3))
                .build();

        RateLimiterConfig heartbeatConfig = RateLimiterConfig.custom()
                .limitForPeriod(safeHeartbeatRpm)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(2))
                .build();
                
        RateLimiterConfig apiConfig = RateLimiterConfig.custom()
                .limitForPeriod(safeApiRpm)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(4))
                .build();
                
        RateLimiterConfig uploadConfig = RateLimiterConfig.custom()
                .limitForPeriod(safeUploadRpm)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(10))
                .build();

        Map<String, RateLimiterConfig> configMap = new HashMap<>();
        configMap.put("global", globalConfig);
        configMap.put("auth", authConfig);
        configMap.put("admin", adminConfig);
        configMap.put("heartbeat", heartbeatConfig);
        configMap.put("api", apiConfig);
        configMap.put("upload", uploadConfig);
        
        logger.info("Production Rate Limiter Registry initialized with {} configurations", configMap.size());
        return RateLimiterRegistry.of(configMap);
    }

    @Bean
    public RateLimiter globalRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("global");
    }

    @Bean
    public RateLimiter authRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("auth");
    }

    @Bean
    public RateLimiter adminRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("admin");
    }

    @Bean
    public RateLimiter heartbeatRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("heartbeat");
    }
    
    @Bean
    public RateLimiter apiRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("api");
    }
    
    @Bean
    public RateLimiter uploadRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("upload");
    }
    
    /**
     * Circuit breaker configuration for fault tolerance
     */
    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(circuitBreakerFailureThreshold)
                .waitDurationInOpenState(Duration.ofMillis(circuitBreakerWaitDurationOpenMs))
                .slidingWindowSize(circuitBreakerSlidingWindowSize)
                .minimumNumberOfCalls(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }
    
    /**
     * Bulkhead configuration for isolation
     */
    @Bean
    public BulkheadConfig defaultBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(100)
                .maxWaitDuration(Duration.ofSeconds(5))
                .build();
    }
    
    /**
     * Time limiter configuration for timeout handling
     */
    @Bean
    public TimeLimiterConfig defaultTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))
                .cancelRunningFuture(true)
                .build();
    }
    
    /**
     * Intelligent retry configuration for different services
     */
    @Bean
    public Map<String, RetryConfig> retryConfigs() {
        Map<String, RetryConfig> configs = new HashMap<>();
        
        configs.put("database", createIntelligentRetryConfig("database"));
        configs.put("redis", createIntelligentRetryConfig("redis"));
        configs.put("kafka", createIntelligentRetryConfig("kafka"));
        configs.put("external-api", createIntelligentRetryConfig("external-api"));
        configs.put("email", createIntelligentRetryConfig("email"));
        
        logger.info("Intelligent retry configurations initialized for {} services", configs.size());
        return configs;
    }
    
    /**
     * Get retry statistics for monitoring
     */
    public Map<String, Integer> getRetryStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        retryCounters.forEach((service, counter) -> {
            stats.put(service, counter.get());
        });
        return stats;
    }
    
    /**
     * Reset retry counters (useful for monitoring)
     */
    public void resetRetryCounters() {
        retryCounters.clear();
        lastRetryTime.clear();
        logger.info("Retry counters reset");
    }
}
