package com.example.smartAttendence.dto.v1;

import java.time.Instant;
import java.util.UUID;

public record HallPassStatusDTO(
    UUID requestId,
    UUID studentId,
    UUID sessionId,
    String studentName,
    String reason,
    int requestedMinutes,
    String status, // PENDING, APPROVED, DENIED
    Instant requestedAt,
    Instant processedAt,
    String processedBy, // FACULTY or AI
    String facultyNotes
) {}
