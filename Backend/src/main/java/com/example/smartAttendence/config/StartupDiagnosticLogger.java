package com.example.smartAttendence.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupDiagnosticLogger {
    private static final Logger logger = LoggerFactory.getLogger(StartupDiagnosticLogger.class);

    @EventListener(ApplicationStartedEvent.class)
    public void onStarted() {
        logger.info("📢 [DIAGNOSTIC] Application Context Refreshed. Preparing to start Tomcat...");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        logger.info("🚀 [DIAGNOSTIC] APPLICATION IS FULLY READY AND LISTENING!");
    }
}
