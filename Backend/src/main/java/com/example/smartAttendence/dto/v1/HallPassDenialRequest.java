package com.example.smartAttendence.dto.v1;

import java.util.UUID;

public record HallPassDenialRequest(
        UUID studentId,
        UUID sessionId,
        String reason,
        String facultyNotes
) {}
