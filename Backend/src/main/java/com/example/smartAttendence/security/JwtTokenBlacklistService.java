package com.example.smartAttendence.security;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * 🔐 JWT TOKEN BLACKLIST SERVICE - FIREBASE EDITION
 * 
 * Prevents reuse of compromised tokens with Firestore-based blacklist
 */
@Service
public class JwtTokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenBlacklistService.class);

    private final Firestore firestore;

    private static final String BLACKLIST_COLLECTION = "blacklisted_tokens";
    private static final String USER_BLACKLIST_COLLECTION = "blacklisted_users";
    
    // 🚀 PERFORMANCE OPTIMIZATION - Standard blacklist duration
    private static final Duration DEFAULT_DURATION = Duration.ofDays(14);

    public JwtTokenBlacklistService(@Nullable Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * 🔐 BLACKLIST TOKEN - Enhanced with hash-based identification
     */
    public void blacklistToken(String token, String reason) {
        if (firestore == null) return;
        try {
            String tokenHash = generateTokenHash(token);
            saveToBlacklist(tokenHash, DEFAULT_DURATION, reason);
            logger.info("🚫 Token blacklisted successfully - Hash: {}, Reason: {}", 
                       tokenHash.substring(0, 8) + "...", reason);
        } catch (Exception e) {
            logger.error("❌ Failed to blacklist token: {}", e.getMessage());
        }
    }

    public void blacklistToken(String token) {
        blacklistToken(token, "legacy");
    }

    public void blacklistTokenById(String tokenId, Instant expiration, String reason) {
        if (firestore == null) return;
        try {
            Duration duration = Duration.between(Instant.now(), expiration);
            if (duration.isPositive()) {
                saveToBlacklist(tokenId, duration, reason);
                logger.info("🚫 Token ID blacklisted - ID: {}, Reason: {}", tokenId, reason);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to blacklist token by ID: {}", e.getMessage());
        }
    }

    public void blacklistTokenById(String tokenId, Instant expiration) {
        blacklistTokenById(tokenId, expiration, "legacy");
    }

    public void blacklistUserTokens(String userId, String reason) {
        if (firestore == null) return;
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("reason", reason);
            data.put("blacklistedAt", com.google.cloud.Timestamp.now());
            data.put("expiresAt", com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
                Instant.now().plus(DEFAULT_DURATION).getEpochSecond(), 0));

            firestore.collection(USER_BLACKLIST_COLLECTION).document(userId).set(data);
            logger.info("🚫 All tokens blacklisted for user - ID: {}, Reason: {}", userId, reason);
        } catch (Exception e) {
            logger.error("❌ Failed to blacklist user tokens: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String tokenId) {
        if (firestore == null) return false;
        try {
            DocumentReference docRef = firestore.collection(BLACKLIST_COLLECTION).document(tokenId);
            var snapshot = docRef.get().get();
            if (snapshot.exists()) {
                com.google.cloud.Timestamp expiresAt = snapshot.getTimestamp("expiresAt");
                return expiresAt != null && expiresAt.compareTo(com.google.cloud.Timestamp.now()) > 0;
            }
            return false;
        } catch (Exception e) {
            logger.error("❌ Failed to check token blacklist: {}", e.getMessage());
            return false;
        }
    }

    public boolean areUserTokensBlacklisted(String userId) {
        if (firestore == null) return false;
        try {
            DocumentReference docRef = firestore.collection(USER_BLACKLIST_COLLECTION).document(userId);
            var snapshot = docRef.get().get();
            if (snapshot.exists()) {
                com.google.cloud.Timestamp expiresAt = snapshot.getTimestamp("expiresAt");
                return expiresAt != null && expiresAt.compareTo(com.google.cloud.Timestamp.now()) > 0;
            }
            return false;
        } catch (Exception e) {
            logger.error("❌ Failed to check user blacklist: {}", e.getMessage());
            return false;
        }
    }

    private void saveToBlacklist(String id, Duration duration, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        data.put("blacklistedAt", com.google.cloud.Timestamp.now());
        data.put("expiresAt", com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
            Instant.now().plus(duration).getEpochSecond(), 0));

        firestore.collection(BLACKLIST_COLLECTION).document(id).set(data);
    }

    private String generateTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("❌ Failed to generate token hash: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    public void cleanupExpiredTokens() {
        // Handled by Firestore TTL policy if configured, or manual batch delete
    }

    public BlacklistStats getBlacklistStats() {
        // Implementation for stats would require count() queries or metadata docs
        return new BlacklistStats(0, 0);
    }

    public static class BlacklistStats {
        private final int blacklistedTokens;
        private final int blacklistedUsers;

        public BlacklistStats(int blacklistedTokens, int blacklistedUsers) {
            this.blacklistedTokens = blacklistedTokens;
            this.blacklistedUsers = blacklistedUsers;
        }

        public int getBlacklistedTokens() { return blacklistedTokens; }
        public int getBlacklistedUsers() { return blacklistedUsers; }
    }
}
