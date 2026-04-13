package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.v1.ExamBarcodeScanRequest;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.service.v1.SharedUtilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExamDayService {

    private final ClassroomSessionV1Repository sessionRepository;
    private final UserV1Repository userRepository;
    private final AttendanceRecordV1Repository attendanceRepository;
    private final SharedUtilityService sharedUtilityService;

    public ExamDayService(
            ClassroomSessionV1Repository sessionRepository,
            UserV1Repository userRepository,
            AttendanceRecordV1Repository attendanceRepository,
            SharedUtilityService sharedUtilityService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.sharedUtilityService = sharedUtilityService;
    }

    /**
     * Process barcode scan for exam day attendance
     */
    public ExamScanResult processBarcodeScan(ExamBarcodeScanRequest request) {
        // Validate session exists and is exam day
        ClassroomSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.sessionId()));

        if (!session.getIsExamDay()) {
            throw new IllegalArgumentException("This is not an exam day session");
        }

        if (!session.isActive()) {
            throw new IllegalArgumentException("Session is not active");
        }

        // Find student by barcode/registration number
        User student = userRepository.findByRegistrationNumber(request.barcodeData())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with registration number: " + request.barcodeData()));

        if (!Role.STUDENT.equals(student.getRole())) {
            throw new IllegalArgumentException("Invalid student registration number");
        }

        // Check if student is already scanned for this session
        if (attendanceRepository.existsBySession_IdAndStudent_Id(request.sessionId(), student.getId())) {
            return new ExamScanResult(
                student.getId(),
                student.getName(),
                student.getRegistrationNumber(),
                "ALREADY_SCANNED",
                LocalDateTime.now()
            );
        }

        // Create attendance record for exam
        AttendanceRecord record = new AttendanceRecord();
        record.setSession(session);
        record.setStudent(student);
        record.setStatus("PRESENT");
        record.setIpAddress("EXAM_BARCODE_SCAN");
        record.setMocked(false);
        record.setRecordedAt(Instant.ofEpochSecond(request.scanTime().toEpochSecond(java.time.ZoneOffset.UTC)));
        record.setBiometricSignature("BARCODE_VERIFIED");

        attendanceRepository.save(record);

        return new ExamScanResult(
            student.getId(),
            student.getName(),
            student.getRegistrationNumber(),
            "SCANNED_SUCCESSFULLY",
            LocalDateTime.now()
        );
    }

    /**
     * Get exam session details
     */
    public ExamSessionDetails getExamSession(String sessionId) {
        UUID sessionUuid = UUID.fromString(sessionId);
        ClassroomSession session = sessionRepository.findById(sessionUuid)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!session.getIsExamDay()) {
            throw new IllegalArgumentException("This is not an exam day session");
        }

        // Count total students in section
        int totalStudents = session.getSection() != null ? 
            Math.toIntExact(sharedUtilityService.countStudentsBySection(session.getSection().getId())) : 0;

        // Count scanned students
        int scannedStudents = Math.toIntExact(attendanceRepository.findAll().stream()
                .filter(ar -> sessionUuid.equals(ar.getSession().getId()) && "PRESENT".equals(ar.getStatus()))
                .count());

        return new ExamSessionDetails(
            session.getId(),
            session.getSubject(),
            session.getRoom(),
            LocalDateTime.ofInstant(session.getStartTime(), java.time.ZoneId.systemDefault()),
            LocalDateTime.ofInstant(session.getEndTime(), java.time.ZoneId.systemDefault()),
            totalStudents,
            scannedStudents,
            session.isActive()
        );
    }

    /**
     * Get all exam sessions for today for current faculty
     */
    public List<ExamSessionDetails> getTodayExamSessions() {
        // TODO: Implement based on current authenticated faculty
        // For now, return empty list - would need faculty context from security
        return List.of();
    }

    // Result records
    public record ExamScanResult(
        UUID studentId,
        String studentName,
        String registrationNumber,
        String status,
        LocalDateTime scanTime
    ) {
        public UUID getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getRegistrationNumber() { return registrationNumber; }
        public String getStatus() { return status; }
        public LocalDateTime getScanTime() { return scanTime; }
    }

    public record ExamSessionDetails(
        UUID id,
        String subject,
        com.example.smartAttendence.entity.Room room,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int totalStudents,
        int scannedStudents,
        boolean active
    ) {
        public UUID getId() { return id; }
        public String getSubject() { return subject; }
        public com.example.smartAttendence.entity.Room getRoom() { return room; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getTotalStudents() { return totalStudents; }
        public int getScannedStudents() { return scannedStudents; }
        public boolean isActive() { return active; }
    }
}
