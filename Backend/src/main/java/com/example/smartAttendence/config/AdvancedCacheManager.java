package com.example.smartAttendence.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced Local Caching Strategy for Smart Attendance System
 * High Performance, Low Latency, No External Dependencies (Redis-Free)
 */
@Component
@ConfigurationProperties(prefix = "cache.advanced")
public class AdvancedCacheManager {

    private final Cache<String, Object> localCache;
    private final MeterRegistry meterRegistry;
    
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);
    
    private static final String CACHE_PREFIX = "smart_attendance:";
    
    @Autowired
    public AdvancedCacheManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats()
                .build();
        initializeMetrics();
    }

    private void initializeMetrics() {
        meterRegistry.gauge("cache.hit_ratio", this, AdvancedCacheManager::getHitRatio);
        meterRegistry.gauge("cache.total_operations", this, AdvancedCacheManager::getTotalOperations);
    }

    public Object get(String key, CacheCategory category) {
        String fullKey = CACHE_PREFIX + category.getPrefix() + key;
        Object value = localCache.getIfPresent(fullKey);
        
        if (value != null) {
            cacheHits.incrementAndGet();
            recordAccessTime(category, true);
            return value;
        } else {
            cacheMisses.incrementAndGet();
            recordAccessTime(category, false);
            return null;
        }
    }

    public void put(String key, Object value, CacheCategory category) {
        String fullKey = CACHE_PREFIX + category.getPrefix() + key;
        localCache.put(fullKey, value);
    }

    public void evict(String key, CacheCategory category) {
        String fullKey = CACHE_PREFIX + category.getPrefix() + key;
        localCache.invalidate(fullKey);
        cacheEvictions.incrementAndGet();
    }

    public void evictCategory(CacheCategory category) {
        localCache.asMap().keySet().removeIf(k -> k.startsWith(CACHE_PREFIX + category.getPrefix()));
        cacheEvictions.incrementAndGet();
    }

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

    private void recordAccessTime(CacheCategory category, boolean isHit) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("cache.access.time")
                .tag("category", category.name())
                .tag("result", isHit ? "hit" : "miss")
                .register(meterRegistry));
    }

    public double getHitRatio() {
        long totalOps = cacheHits.get() + cacheMisses.get();
        return totalOps > 0 ? (cacheHits.get() * 100.0) / totalOps : 0.0;
    }

    public double getTotalOperations() {
        return cacheHits.get() + cacheMisses.get();
    }

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
        CacheCategory(String prefix) { this.prefix = prefix; }
        public String getPrefix() { return prefix; }
    }

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

        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public double getHitRatio() { return hitRatio; }
        public long getTotalOperations() { return totalOperations; }
    }
}
