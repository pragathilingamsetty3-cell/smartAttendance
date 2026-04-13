package com.example.smartAttendence.domain;

/**
 * Academic status for student profiles
 * Different from UserStatus which is account-level status
 */
public enum AcademicStatus {
    /**
     * Regular student in good standing
     */
    REGULAR,
    
    /**
     * Student on academic probation
     */
    PROBATION,
    
    /**
     * Student temporarily suspended from academics
     */
    ACADEMIC_SUSPENSION,
    
    /**
     * Student taking break from studies
     */
    ACADEMIC_LEAVE,
    
    /**
     * Student has completed all requirements
     */
    COMPLETED,
    
    /**
     * Student has withdrawn from program
     */
    WITHDRAWN
}
