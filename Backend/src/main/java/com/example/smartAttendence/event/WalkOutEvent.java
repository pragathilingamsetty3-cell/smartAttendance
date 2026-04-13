package com.example.smartAttendence.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record WalkOutEvent(
        UUID studentId,
        UUID sessionId,
        LocalDateTime timestamp
) {}

