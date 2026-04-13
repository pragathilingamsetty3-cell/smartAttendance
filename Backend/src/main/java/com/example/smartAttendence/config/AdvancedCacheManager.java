package com.example.smartAttendence.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ultimate Production Caching Strategy for Smart Attendance System
 * Top Tier 1 Multinational Company Grade Performance
 * Top Tier 1 Multinational Company Grade Performance
 */
@Component
@ConfigurationProperties(prefix = "cache.advanced")
public class AdvancedCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    
    // HTTPS Caching Properties
    private boolean httpsCacheEnabled = true;
    private long httpsTtl = 900; // 15 minutes for HTTPS responses
    private boolean enableCompression = true;
    private int maxCacheSize = 10000;
    
    // Cache statistics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);
    
    // Cache configurations - HTTPS Optimized
    private static final String CACHE_PREFIX = "smart_attendance:";
    private static final long DEFAULT_TTL = 1800; // 30 minutes
    private static final long SHORT_TTL = 300;    // 5 minutes
    private static final long LONG_TTL = 3600;     // 1 hour
    private static final long HTTPS_TTL = 900;    // 15 minutes for HTTPS
    
    @Autowired
    public AdvancedCacheManager(RedisTemplate<String, Object> redisTemplate,
                             MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    private void initializeMetrics() {
        meterRegistry.gauge("cache.hit_ratio", this, AdvancedCacheManager::getHitRatio);
        meterRegistry.gauge("cache.total_operations", this, AdvancedCacheManager::getTotalOperations);
        meterRegistry.counter("cache.hits");
        meterRegistry.counter("cache.misses");
        meterRegistry.counter("cache.evictions");
    }

    /**
     * Get value from cache with intelligent TTL selection
     */
    public Object get(String key, CacheCategory category) {
        String fullKey = CACHE_PREFIX + category.getPrefix() + key;
        
        try {
            Object value = redisTemplate.opsForValue().get(fullKey);
            if (value != null) {
                cacheHits.incrementAndGet();
                meterRegistry.counter("cache.hits", "category", category.name()).increment();
                recordAccessTime(category, true);
                return value;
            } else {
                cacheMisses.incrementAndGet();
                meterRegistry.counter("cache.misses", "category", category.name()).increment();
                recordAccessTime(category, false);
                return null;
            }
        } catch (Exception e) {
            meterRegistry.counter("cache.errors", "category", category.name()).increment();
            return null;
        }
    }

    /**
     * Put value in cache with category-specific TTL
     */
    public void put(String key, Object value, CacheCategory category) {
        String fullKey = CACHE_PREFIX + category.getPrefix() + key;
        long ttl = getTTLForCategory(category);
        
        try {
            redisTemplate.opsForValue().set(fullKey, value, ttl, TimeUnit.SECONDS);
            meterRegistry.counter("cache.puts", "category", category.name()).increment();
        } catch (Exception e) {
            meterRegistry.counter("cache.errors", "category", category.name()).increment();
        }
    }

    /**
     * Evict specific cache key
     */
    public void evict(String key, CacheCategory category) {
        String fullKey = CACHE_PREFIX + category.getPrefix() + key;
        
        try {
            Boolean deleted = redisTemplate.delete(fullKey);
            if (Boolean.TRUE.equals(deleted)) {
                cacheEvictions.incrementAndGet();
                meterRegistry.counter("cache.evictions", "category", category.name()).increment();
            }
        } catch (Exception e) {
            meterRegistry.counter("cache.errors", "category", category.name()).increment();
        }
    }

    /**
     * Evict all keys in a category
     */
    public void evictCategory(CacheCategory category) {
        String pattern = CACHE_PREFIX + category.getPrefix() + "*";
        
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
            cacheEvictions.incrementAndGet();
            meterRegistry.counter("cache.evictions", "category", category.name(), "scope", "all").increment();
        } catch (Exception e) {
            meterRegistry.counter("cache.errors", "category", category.name()).increment();
        }
    }

    /**
     * Cache warming for frequently accessed data
     */
    public void warmCache(CacheCategory category) {
        // Implement cache warming logic based on category
        switch (category) {
            case USER_SESSIONS:
                warmUserSessions();
                break;
            case DEPARTMENTS:
                warmDepartments();
                break;
            case ATTENDANCE_RECORDS:
                warmAttendanceRecords();
                break;
            case SYSTEM_CONFIG:
                warmSystemConfig();
                break;
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStatistics getStatistics() {
        long totalOps = cacheHits.get() + cacheMisses.get();
        double hitRatio = totalOps > 0 ? (cacheHits.get() * 100.0) / totalOps : 0.0;
        
        return new CacheStatistics(
            cacheHits.get(),
            cacheMisses.get(),
            cacheEvictions.get(),
            hitRatio,
            totalOps
        );
    }

    private long getTTLForCategory(CacheCategory category) {
        switch (category) {
            case USER_SESSIONS:
                return SHORT_TTL; // 5 minutes - sessions change frequently
            case ATTENDANCE_RECORDS:
                return DEFAULT_TTL; // 30 minutes - moderate change frequency
            case DEPARTMENTS:
                return LONG_TTL; // 1 hour - rarely changes
            case SYSTEM_CONFIG:
                return LONG_TTL; // 1 hour - rarely changes
            case HTTPS_RESPONSES:
                return httpsTtl; // 15 minutes - HTTPS responses
            case SSL_SESSIONS:
                return SHORT_TTL; // 5 minutes - SSL sessions
            case PERFORMANCE_METRICS:
                return SHORT_TTL; // 5 minutes - metrics change frequently
            case API_RESPONSES:
                return DEFAULT_TTL; // 30 minutes - API responses
            default:
                return DEFAULT_TTL;
        }
    }

    private void recordAccessTime(CacheCategory category, boolean isHit) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("cache.access.time")
                .tag("category", category.name())
                .tag("result", isHit ? "hit" : "miss")
                .register(meterRegistry));
    }

    private void warmUserSessions() {
        // Implement user session warming logic
        // This would typically load active user sessions into cache
    }

    private void warmDepartments() {
        // Implement department data warming logic
        // Pre-load department and section data
    }

    private void warmAttendanceRecords() {
        // Implement attendance records warming logic
        // Pre-load recent attendance data for active sessions
    }

    private void warmSystemConfig() {
        // Implement system config warming logic
        // Pre-load system configuration and settings
    }

    // Public metric methods
    public double getHitRatio() {
        long totalOps = cacheHits.get() + cacheMisses.get();
        return totalOps > 0 ? (cacheHits.get() * 100.0) / totalOps : 0.0;
    }

    public double getTotalOperations() {
        return cacheHits.get() + cacheMisses.get();
    }

    /**
     * Cache categories for intelligent TTL management
     */
    public enum CacheCategory {
        USER_SESSIONS("user_sessions:"),
        ATTENDANCE_RECORDS("attendance:"),
        DEPARTMENTS("departments:"),
        SYSTEM_CONFIG("system_config:"),
        PERFORMANCE_METRICS("metrics:"),
        API_RESPONSES("api_responses:"),
        HTTPS_RESPONSES("https_responses:"),
        SSL_SESSIONS("ssl_sessions:");
        
        private final String prefix;
        
        CacheCategory(String prefix) {
            this.prefix = prefix;
        }
        
        public String getPrefix() {
            return prefix;
        }
    }

    /**
     * Cache statistics data class
     */
    public static class CacheStatistics {
        private final long hits;
        private final long misses;
        private final long evictions;
        private final double hitRatio;
        private final long totalOperations;

        public CacheStatistics(long hits, long misses, long evictions, double hitRatio, long totalOperations) {
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRatio = hitRatio;
            this.totalOperations = totalOperations;
        }

        // Getters
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public double getHitRatio() { return hitRatio; }
        public long getTotalOperations() { return totalOperations; }
    }
}
