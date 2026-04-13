package com.example.smartAttendence.security;

import java.time.Instant;

/**
 * 🔐 PRODUCTION-GRADE SECURITY EVENT
 * 
 * Lightweight security event for real-time threat detection and monitoring
 * Optimized for high-frequency operations with minimal overhead
 */
public class SecurityEvent {
    
    private final Instant timestamp;
    private final String eventType;
    private final String clientIP;
    private final String endpoint;
    private final String description;
    private final String severity;
    
    // 🔐 CONSTRUCTOR
    public SecurityEvent(Instant timestamp, String eventType, String clientIP, 
                         String endpoint, String description) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.eventType = eventType;
        this.clientIP = clientIP;
        this.endpoint = endpoint;
        this.description = description;
        this.severity = determineSeverity(eventType);
    }
    
    // 🔐 CONSTRUCTOR WITH SEVERITY
    public SecurityEvent(Instant timestamp, String eventType, String clientIP, 
                         String endpoint, String description, String severity) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.eventType = eventType;
        this.clientIP = clientIP;
        this.endpoint = endpoint;
        this.description = description;
        this.severity = severity != null ? severity : determineSeverity(eventType);
    }
    
    // 🔐 GETTERS
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getClientIP() {
        return clientIP;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    // 🔐 UTILITY METHODS
    
    /**
     * Determine severity based on event type
     */
    private String determineSeverity(String eventType) {
        if (eventType == null) return "MEDIUM";
        
        String upperType = eventType.toUpperCase();
        if (upperType.contains("CRITICAL") || upperType.contains("BRUTE_FORCE")) {
            return "CRITICAL";
        } else if (upperType.contains("HIGH") || upperType.contains("SUSPICIOUS") || 
                   upperType.contains("THREAT") || upperType.contains("ATTACK")) {
            return "HIGH";
        } else if (upperType.contains("MEDIUM") || upperType.contains("WARNING")) {
            return "MEDIUM";
        } else if (upperType.contains("LOW") || upperType.contains("INFO")) {
            return "LOW";
        }
        return "MEDIUM"; // Default to medium for security events
    }
    
    /**
     * Check if event requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return "CRITICAL".equals(severity) || 
               eventType != null && eventType.toUpperCase().contains("BRUTE_FORCE");
    }
    
    /**
     * Check if event is a threat
     */
    public boolean isThreat() {
        return eventType != null && 
               (eventType.toUpperCase().contains("THREAT") || 
                eventType.toUpperCase().contains("ATTACK") ||
                eventType.toUpperCase().contains("SUSPICIOUS") ||
                eventType.toUpperCase().contains("BRUTE_FORCE"));
    }
    
    /**
     * Get age of the event in seconds
     */
    public long getAgeInSeconds() {
        return Instant.now().getEpochSecond() - timestamp.getEpochSecond();
    }
    
    /**
     * Check if event is recent (within last hour)
     */
    public boolean isRecent() {
        return getAgeInSeconds() < 3600; // 1 hour
    }
    
    /**
     * Get summary of the event
     */
    public String getSummary() {
        return String.format("[%s] %s from %s to %s - %s", 
                           severity, eventType, clientIP, endpoint, description);
    }
    
    @Override
    public String toString() {
        return "SecurityEvent{" +
               "timestamp=" + timestamp +
               ", eventType='" + eventType + '\'' +
               ", clientIP='" + clientIP + '\'' +
               ", endpoint='" + endpoint + '\'' +
               ", description='" + description + '\'' +
               ", severity='" + severity + '\'' +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        SecurityEvent that = (SecurityEvent) o;
        return timestamp.equals(that.timestamp) &&
               eventType.equals(that.eventType) &&
               clientIP.equals(that.clientIP);
    }
    
    @Override
    public int hashCode() {
        int result = timestamp.hashCode();
        result = 31 * result + eventType.hashCode();
        result = 31 * result + clientIP.hashCode();
        return result;
    }
}
