package com.example.smartAttendence.dto.v1;

import java.util.UUID;

public record HallPassApprovalRequest(
        UUID studentId,
        UUID sessionId,
        int approvedMinutes,
        String facultyNotes
) {}
