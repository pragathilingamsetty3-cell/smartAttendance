package com.example.smartAttendence.domain;

/**
 * User status enum for managing student lifecycle
 * Used for soft delete and status tracking
 */
public enum UserStatus {
    /**
     * Student is currently active and attending classes
     */
    ACTIVE,
    
    /**
     * Student is temporarily inactive (e.g., medical leave)
     */
    INACTIVE,
    
    /**
     * Student has dropped out from the institution
     */
    DROPPED_OUT,
    
    /**
     * Student is temporarily suspended
     */
    SUSPENDED,
    
    /**
     * Student has successfully graduated
     */
    GRADUATED,
    
    /**
     * Faculty has transferred to another institution
     */
    TRANSFERRED,
    
    /**
     * Faculty has resigned from their position
     */
    RESIGNED
}
