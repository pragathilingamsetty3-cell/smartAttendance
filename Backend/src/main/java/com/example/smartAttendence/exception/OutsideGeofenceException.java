package com.example.smartAttendence.exception;

public class OutsideGeofenceException extends RuntimeException {
    public OutsideGeofenceException(String message) {
        super(message);
    }
}

