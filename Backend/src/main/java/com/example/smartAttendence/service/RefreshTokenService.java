package com.example.smartAttendence.service;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.entity.RefreshToken;
import com.example.smartAttendence.repository.v1.RefreshTokenV1Repository;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    
    private final RefreshTokenV1Repository refreshTokenV1Repository;

    @Value("${jwt.refresh-token.expiration-days}")
    private int refreshTokenExpirationDays;
    
    @Value("${jwt.refresh-token.warning-hours:6}")
    private int tokenWarningHours;
    
    @Value("${jwt.refresh-token.max-active-per-user:3}")
    private int maxActiveTokensPerUser;
    
    // Cache for token refresh attempts to prevent brute force
    private final ConcurrentHashMap<String, AtomicInteger> refreshAttempts = new ConcurrentHashMap<>();
    
    // Maximum refresh attempts per hour
    private static final int MAX_REFRESH_ATTEMPTS_PER_HOUR = 10;

    public RefreshTokenService(RefreshTokenV1Repository refreshTokenV1Repository) {
        this.refreshTokenV1Repository = refreshTokenV1Repository;
    }

    public RefreshToken createRefreshToken(User user) {
        logger.info("Creating refresh token for user: {}", user.getEmail());
        
        try {
            // Clean up old tokens for this user
            logger.info("🔐 Cleaning up old tokens for user: {}", user.getEmail());
            cleanupOldTokens(user);
            logger.info("🔐 Old tokens cleaned up successfully");
            
            logger.info("🔐 Creating new refresh token object");
            RefreshToken token = new RefreshToken();
            token.setUser(user);
            token.setExpiryDate(Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS));
            token.setToken(UUID.randomUUID().toString());
            
            logger.info("🔐 Saving refresh token to database");
            RefreshToken savedToken = refreshTokenV1Repository.save(token);
            logger.info("🔐 Refresh token saved successfully: {}", savedToken.getToken());
            
            logger.info("Successfully created refresh token for user: {}, expires at: {}", 
                       user.getEmail(), savedToken.getExpiryDate());
            
            return savedToken;
        } catch (Exception e) {
            logger.error("🔐 Error creating refresh token for user: {}", user.getEmail(), e);
            throw e;
        }
    }

    public void verifyExpiration(RefreshToken token) {
        Instant now = Instant.now();
        Instant expiryTime = token.getExpiryDate();
        
        // Check if token is expired
        if (expiryTime.compareTo(now) < 0) {
            logger.warn("🚨 REFRESH TOKEN EXPIRED: User: {}, Expired at: {}, Current system time: {}", 
                       token.getUser().getEmail(), expiryTime, now);
            refreshTokenV1Repository.delete(token);
            throw new RuntimeException("Refresh token expired. Please login again.");
        }
        
        // Check if token is about to expire (within warning window)
        Instant warningTime = now.plus(tokenWarningHours, ChronoUnit.HOURS);
        if (expiryTime.compareTo(warningTime) < 0) {
            logger.info("Refresh token for user: {} is about to expire at: {}", 
                       token.getUser().getEmail(), expiryTime);
        }
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenV1Repository.findByToken(token);
    }
    
    /**
     * Enhanced token refresh with rate limiting and security checks
     */
    @Transactional
    public RefreshToken refreshToken(String oldTokenString) {
        // Check refresh attempts to prevent brute force
        AtomicInteger attempts = refreshAttempts.computeIfAbsent(oldTokenString, k -> new AtomicInteger(0));
        if (attempts.incrementAndGet() > MAX_REFRESH_ATTEMPTS_PER_HOUR) {
            logger.warn("Too many refresh attempts for token: {}", oldTokenString.substring(0, 8) + "...");
            throw new RuntimeException("Too many refresh attempts. Please login again.");
        }
        
        Optional<RefreshToken> tokenOpt = refreshTokenV1Repository.findByToken(oldTokenString);
        if (tokenOpt.isEmpty()) {
            logger.warn("Invalid refresh token attempted: {}", oldTokenString.substring(0, 8) + "...");
            throw new RuntimeException("Invalid refresh token");
        }
        
        RefreshToken oldToken = tokenOpt.get();
        verifyExpiration(oldToken);
        
        // Create new token
        RefreshToken newToken = createRefreshToken(oldToken.getUser());
        
        // Delete old token
        refreshTokenV1Repository.delete(oldToken);
        
        logger.info("Successfully refreshed token for user: {}", oldToken.getUser().getEmail());
        return newToken;
    }

    @Transactional
    public void deleteByUser(User user) {
        logger.info("Deleting all refresh tokens for user: {}", user.getEmail());
        refreshTokenV1Repository.deleteByUser(user);
        // Clear refresh attempts cache for this user
        refreshAttempts.entrySet().removeIf(entry -> {
            Optional<RefreshToken> token = findByToken(entry.getKey());
            return token.isPresent() && token.get().getUser().getId().equals(user.getId());
        });
    }
    
    /**
     * Clean up old tokens for a user to prevent token accumulation
     */
    @Transactional
    public void cleanupOldTokens(User user) {
        // Use the correct method that returns a list
        List<RefreshToken> userTokens = refreshTokenV1Repository.findByUserAndExpiryDateAfter(user, Instant.now());
        
        if (!userTokens.isEmpty() && userTokens.size() >= maxActiveTokensPerUser) {
            // Sort by expiry date and remove oldest tokens
            userTokens.sort((t1, t2) -> t1.getExpiryDate().compareTo(t2.getExpiryDate()));
            
            int tokensToRemove = userTokens.size() - maxActiveTokensPerUser + 1;
            for (int i = 0; i < tokensToRemove; i++) {
                RefreshToken tokenToRemove = userTokens.get(i);
                refreshTokenV1Repository.delete(tokenToRemove);
                logger.info("Cleaned up old token for user: {}, token expired at: {}", 
                           user.getEmail(), tokenToRemove.getExpiryDate());
            }
        }
    }
    
    /**
     * Scheduled cleanup of expired tokens
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Starting scheduled cleanup of expired refresh tokens");
        
        List<RefreshToken> expiredTokens = refreshTokenV1Repository.findByExpiryDateBefore(Instant.now());
        
        if (!expiredTokens.isEmpty()) {
            refreshTokenV1Repository.deleteAll(expiredTokens);
            logger.info("Cleaned up {} expired refresh tokens", expiredTokens.size());
        }
        
        // Reset refresh attempts counter periodically
        refreshAttempts.clear();
        logger.debug("Reset refresh attempts counter");
    }
    
    /**
     * Get token information for monitoring
     */
    public int getActiveTokenCount(User user) {
        return refreshTokenV1Repository.findByUser(user).isPresent() ? 1 : 0;
    }
    
    /**
     * Check if user has active tokens
     */
    public boolean hasActiveTokens(User user) {
        return !refreshTokenV1Repository.findByUserAndExpiryDateAfter(user, Instant.now()).isEmpty();
    }
}
