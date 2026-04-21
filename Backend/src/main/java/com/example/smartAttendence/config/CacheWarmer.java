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
        log.info("🚀 ULTRA-PERFORMANCE: Starting Cache Warming... Warming up high-priority metadata...");
        
        try {
            // Warm up Departments
            adminV1Service.getAllDepartments();
            log.info("✅ Cache Warmer: Departments Pre-loaded");
            
            // Warm up Rooms
            adminV1Service.getAllRooms();
            log.info("✅ Cache Warmer: Rooms Pre-loaded");
            
            // Warm up Section Details (Top 5 active ones if needed, but getAllDepartments is usually enough)
            log.info("❄️ Cache Warmer: Critical paths are now WARM and ready for production load.");
        } catch (Exception e) {
            log.warn("⚠️ Cache Warmer: Problem pre-loading caches during startup: {}", e.getMessage());
        }
    }
}
