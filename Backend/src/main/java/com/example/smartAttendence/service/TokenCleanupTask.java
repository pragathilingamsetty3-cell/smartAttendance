package com.example.smartAttendence.service;

import com.example.smartAttendence.repository.v1.RefreshTokenV1Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
@Slf4j
public class TokenCleanupTask {

    private final RefreshTokenV1Repository refreshTokenV1Repository;

    public TokenCleanupTask(RefreshTokenV1Repository refreshTokenV1Repository) {
        this.refreshTokenV1Repository = refreshTokenV1Repository;
    }

    // 🚀 Runs every night at midnight to delete tokens older than 4 months
    @Scheduled(cron = "0 0 0 * * ?") 
    @Transactional
    public void cleanExpiredTokens() {
        refreshTokenV1Repository.deleteByExpiryDateBefore(Instant.now());
        log.info("Cleanup: Old refresh tokens purged from database.");
    }
}