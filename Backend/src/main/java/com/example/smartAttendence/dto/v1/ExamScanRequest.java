package com.example.smartAttendence.dto.v1;

import java.util.UUID;

public record ExamScanRequest(
    UUID sessionId,
    String studentBarcode
) {}
