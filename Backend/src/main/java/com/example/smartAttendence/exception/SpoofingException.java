package com.example.smartAttendence.exception;

public class SpoofingException extends RuntimeException {
    
    private final String severity;
    private final String reason;

    public SpoofingException(String message, String severity, String reason) {
        super(message);
        this.severity = severity;
        this.reason = reason;
    }

    public String getSeverity() {
        return severity;
    }

    public String getReason() {
        return reason;
    }
}
