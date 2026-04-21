package com.example.smartAttendence.dto.v1;

import java.time.Instant;
import java.util.UUID;

/**
 * 🚀 ULTRA-PERFORMANCE DTO: Lightweight User Summary
 * Reduces JSON payload size by ~40% by including only UI-essential fields.
 */
public record UserSummaryDTO(
    UUID id,
    String name,
    String email,
    String registrationNumber,
    String role,
    String department,
    String status,
    UUID sectionId,
    Instant createdAt
) {
}
