package com.example.smartAttendence.config;

import org.springframework.web.servlet.HandlerInterceptor;
import com.example.smartAttendence.security.SecurityAuditLogger;
import com.example.smartAttendence.security.SecurityHeadersMonitor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 🔐 PRODUCTION SECURITY MONITORING CONFIGURATION
 * 
 * Registers consolidated security monitoring for 10K+ concurrent users
 * Uses unified SecurityAuditLogger for comprehensive audit logging, performance monitoring, and threat detection
 * Includes security headers validation for complete coverage
 */
@Configuration
public class SecurityMonitoringConfig implements WebMvcConfigurer {

    private final HandlerInterceptor securityAuditLogger;
    private final HandlerInterceptor securityHeadersMonitor;

    public SecurityMonitoringConfig(HandlerInterceptor securityAuditLogger, 
                                   HandlerInterceptor securityHeadersMonitor) {
        this.securityAuditLogger = securityAuditLogger;
        this.securityHeadersMonitor = securityHeadersMonitor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register consolidated security audit logger for comprehensive monitoring
        registry.addInterceptor(securityAuditLogger)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/actuator/health",
                    "/actuator/info",
                    "/error",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                );
                
        // Register security headers monitor for all endpoints
        registry.addInterceptor(securityHeadersMonitor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/actuator/health",
                    "/actuator/info",
                    "/error",
                    "/v3/api-docs/**",
                    "/swagger-ui/**"
                );
    }
}
