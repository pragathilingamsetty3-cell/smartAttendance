package com.example.smartAttendence.security;

import java.time.Instant;
import java.util.Map;

/**
 * 🔐 PRODUCTION-GRADE SECURITY AUDIT EVENT
 * 
 * Comprehensive security audit event tracking for Tier-1 production compliance
 * Optimized for high-performance logging with minimal memory footprint
 */
public class SecurityAuditEvent {
    
    private String eventId;
    private String eventType;
    private String clientIP;
    private String endpoint;
    private String action;
    private Instant timestamp;
    private Map<String, String> details;
    
    // 🔐 DEFAULT CONSTRUCTOR
    public SecurityAuditEvent() {
        this.timestamp = Instant.now();
    }
    
    // 🔐 FULL CONSTRUCTOR
    public SecurityAuditEvent(String eventId, String eventType, String clientIP, 
                              String endpoint, String action, Instant timestamp, 
                              Map<String, String> details) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.clientIP = clientIP;
        this.endpoint = endpoint;
        this.action = action;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.details = details;
    }
    
    // 🔐 GETTERS AND SETTERS
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getClientIP() {
        return clientIP;
    }
    
    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, String> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
    
    // 🔐 UTILITY METHODS
    
    /**
     * Get event severity based on event type
     */
    public String getSeverity() {
        if (eventType == null) return "UNKNOWN";
        
        String upperType = eventType.toUpperCase();
        if (upperType.contains("CRITICAL") || upperType.contains("BRUTE_FORCE")) {
            return "CRITICAL";
        } else if (upperType.contains("HIGH") || upperType.contains("SUSPICIOUS")) {
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
        return "CRITICAL".equals(getSeverity()) || 
               eventType != null && eventType.toUpperCase().contains("BRUTE_FORCE");
    }
    
    /**
     * Get summary of the event for logging
     */
    public String getSummary() {
        return String.format("[%s] %s from %s to %s - %s", 
                           eventType, clientIP, endpoint, action, getSeverity());
    }
    
    @Override
    public String toString() {
        return "SecurityAuditEvent{" +
               "eventId='" + eventId + '\'' +
               ", eventType='" + eventType + '\'' +
               ", clientIP='" + clientIP + '\'' +
               ", endpoint='" + endpoint + '\'' +
               ", action='" + action + '\'' +
               ", timestamp=" + timestamp +
               ", severity=" + getSeverity() +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        SecurityAuditEvent that = (SecurityAuditEvent) o;
        return eventId != null ? eventId.equals(that.eventId) : that.eventId == null;
    }
    
    @Override
    public int hashCode() {
        return eventId != null ? eventId.hashCode() : 0;
    }
}
