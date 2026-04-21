package com.example.smartAttendence.config;

import com.example.smartAttendence.service.v1.AdminV1Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * ❄️ ULTRA-PERFORMANCE: Startup Cache Warmer
 * Pre-loads critical metadata (Departments, Rooms) into Caffeine cache on startup.
 * This ensures the first admin user experiences 0ms latency.
 */
@Component
@Slf4j
public class CacheWarmer {

    private final AdminV1Service adminV1Service;

    public CacheWarmer(AdminV1Service adminV1Service) {
        this.adminV1Service = adminV1Service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 🚀 Defer warming to prevent "Cold Boot" congestion
        Thread.ofVirtual().start(() -> {
            try {
                log.info("⏳ [STANDBY] Delaying Cache Warmer for 5s to allow system stabilization...");
                Thread.sleep(5000);
                
                log.info("🚀 ULTRA-PERFORMANCE: Starting Cache Warming... Warming up high-priority metadata...");
                
                // Warm up Departments
                adminV1Service.getAllDepartments();
                log.info("✅ Cache Warmer: Departments Pre-loaded");
                
                // Warm up Rooms
                adminV1Service.getAllRooms();
                log.info("✅ Cache Warmer: Rooms Pre-loaded");
                
                log.info("❄️ Cache Warmer: Critical paths are now WARM and ready for production load.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("⚠️ Cache Warmer: Problem pre-loading caches during startup: {}", e.getMessage());
            }
        });
    }
}
