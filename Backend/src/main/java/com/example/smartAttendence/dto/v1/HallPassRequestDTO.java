package com.example.smartAttendence.dto.v1;

import java.util.UUID;

public record HallPassRequestDTO(
        UUID studentId,
        UUID sessionId,
        String reason,
        int requestedMinutes,
        String studentNotes
) {}
