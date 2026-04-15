package com.example.smartAttendence.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FilterCacheConfig {

    /**
     * 🛡️ L1 Rate Limit Cache (1-minute window)
     * Reduces Redis round-trips for high-frequency users.
     */
    @Bean
    public Cache<String, Boolean> rateLimitCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(5000) // Support up to 5k concurrent users
                .build();
    }

    /**
     * 🔐 L1 Device Verification Cache (5-minute window)
     * Makes navigating between dashboard tabs instant.
     */
    @Bean
    public Cache<String, Boolean> deviceVerificationCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    /**
     * 🧠 L1 Threat Analysis Cache (5-minute window)
     * Prevents re-analyzing the same IP/UA combinations repeatedly.
     */
    @Bean
    public Cache<String, Integer> threatAnalysisCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(2000)
                .build();
    }
}
