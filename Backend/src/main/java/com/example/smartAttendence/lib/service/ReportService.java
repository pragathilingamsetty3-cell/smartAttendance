package com.example.smartAttendence.lib.service;

import lombok.extern.slf4j.Slf4j;
import com.example.smartAttendence.domain.AttendanceRecord;
import com.example.smartAttendence.domain.ClassroomSession;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.service.EmailService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ReportService {

    private final AttendanceRecordV1Repository attendanceRecordRepository;
    private final ClassroomSessionV1Repository classroomSessionRepository;
    private final UserV1Repository userRepository;
    private final SectionRepository sectionRepository;
    private final EmailService emailService;
    private final com.example.smartAttendence.util.SecurityUtils securityUtils;
    private final com.example.smartAttendence.repository.DepartmentRepository departmentRepository;

    public ReportService(AttendanceRecordV1Repository attendanceRecordRepository,
                       ClassroomSessionV1Repository classroomSessionRepository,
                       UserV1Repository userRepository,
                       SectionRepository sectionRepository,
                       EmailService emailService,
                       com.example.smartAttendence.util.SecurityUtils securityUtils,
                       com.example.smartAttendence.repository.DepartmentRepository departmentRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.classroomSessionRepository = classroomSessionRepository;
        this.userRepository = userRepository;
        this.sectionRepository = sectionRepository;
        this.emailService = emailService;
        this.securityUtils = securityUtils;
        this.departmentRepository = departmentRepository;
    }

    @Scheduled(cron = "0 0 18 * * SAT") // Every Saturday at 6 PM
    public void generateWeeklyReports() {
        log.info("Starting weekly attendance report generation...");
        
        try {
            // Get the past week's date range (Monday to Sunday)
            LocalDate weekStart = getPreviousMonday();
            LocalDate weekEnd = weekStart.plusDays(6);
            
            Instant startTime = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant endTime = weekEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
            
            // Get all sessions for the week
            List<ClassroomSession> weeklySessions = classroomSessionRepository
                    .findByStartTimeBetween(startTime, endTime);
            
            // Group sessions by faculty for individual reports
            Map<UUID, List<ClassroomSession>> sessionsByFaculty = weeklySessions.stream()
                    .collect(Collectors.groupingBy(session -> session.getFaculty().getId()));
            
            for (Map.Entry<UUID, List<ClassroomSession>> entry : sessionsByFaculty.entrySet()) {
                UUID facultyId = entry.getKey();
                List<ClassroomSession> facultySessions = entry.getValue();
                
                byte[] excelReport = generateExcelReport(facultySessions, weekStart, weekEnd);
                
                // TODO: Send email with Excel attachment to faculty
                // sendEmailWithReport(facultyId, excelReport, weekStart, weekEnd);
                
                log.info("Generated weekly report for faculty ID: {}", facultyId);
            }
            
            log.info("Weekly attendance report generation completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to generate weekly reports", e);
        }
    }

    private byte[] generateExcelReport(List<ClassroomSession> sessions, LocalDate weekStart, LocalDate weekEnd) 
            throws IOException {
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); // Stream rows to disk after 100 in-memory
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // Create summary sheet
            Sheet summarySheet = workbook.createSheet("Weekly Summary");
            createSummarySheet(summarySheet, sessions, weekStart, weekEnd);
            
            // Create detailed attendance sheets
            for (ClassroomSession session : sessions) {
                String sheetName = sanitizeSheetName(session.getTimetable().getSubject() + " " + 
                                                   session.getStartTime().atZone(ZoneId.systemDefault()).toLocalDate());
                Sheet detailSheet = workbook.createSheet(sheetName);
                createDetailSheet(detailSheet, session);
            }
            
            // ⚠️ SXSSF Limitation: autoSizeColumn is expensive and requires all rows in track.
            // For 512MB RAM, we only auto-size the most important columns to avoid OOM.
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet instanceof SXSSFSheet sxSheet) {
                    sxSheet.trackAllColumnsForAutoSizing();
                    autoSizeColumns(sxSheet);
                }
            }
            
            workbook.write(outputStream);
            workbook.dispose(); // CRITICAL: Deletes temporary XML files from disk
            return outputStream.toByteArray();
        }
    }

    private void createSummarySheet(Sheet sheet, List<ClassroomSession> sessions, 
                                  LocalDate weekStart, LocalDate weekEnd) {
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Subject", "Date", "Time", "Room", "Total Students", "Present", "Absent", "Attendance %"};
        
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Create data rows
        int rowNum = 1;
        for (ClassroomSession session : sessions) {
            Row row = sheet.createRow(rowNum++);
            
            List<AttendanceRecord> records = attendanceRecordRepository
                    .findBySessionIdOrderByRecordedAtAsc(session.getId());
            
            long totalStudents = records.size();
            long presentCount = records.stream()
                    .filter(r -> "PRESENT".equals(r.getStatus()))
                    .count();
            double attendancePercentage = totalStudents > 0 ? (presentCount * 100.0 / totalStudents) : 0.0;
            
            row.createCell(0).setCellValue(session.getTimetable().getSubject());
            row.createCell(1).setCellValue(session.getStartTime()
                    .atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            row.createCell(2).setCellValue(session.getStartTime()
                    .atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            row.createCell(3).setCellValue(session.getRoom().getName());
            row.createCell(4).setCellValue(totalStudents);
            row.createCell(5).setCellValue(presentCount);
            row.createCell(6).setCellValue(totalStudents - presentCount);
            row.createCell(7).setCellValue(String.format("%.1f%%", attendancePercentage));
        }
        
        // Add summary statistics at the bottom
        Row summaryRow = sheet.createRow(rowNum + 2);
        summaryRow.createCell(0).setCellValue("WEEKLY TOTAL");
        summaryRow.createCell(4).setCellValue(sessions.stream().mapToLong(s -> 
                attendanceRecordRepository.findBySessionIdOrderByRecordedAtAsc(s.getId()).size()).sum());
        summaryRow.createCell(5).setCellValue(sessions.stream().mapToLong(s -> 
                attendanceRecordRepository.findBySessionIdOrderByRecordedAtAsc(s.getId()).stream()
                        .filter(r -> "PRESENT".equals(r.getStatus())).count()).sum());
    }

    private void createDetailSheet(Sheet sheet, ClassroomSession session) {
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Student Name", "Registration Number", "Status", "Marked At", "IP Address", "Biometric Verified"};
        
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Create data rows
        List<AttendanceRecord> records = attendanceRecordRepository
                .findBySessionIdOrderByRecordedAtAsc(session.getId());
        
        int rowNum = 1;
        for (AttendanceRecord record : records) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(record.getStudent().getName());
            row.createCell(1).setCellValue(record.getStudent().getRegistrationNumber());
            row.createCell(2).setCellValue(record.getStatus());
            row.createCell(3).setCellValue(record.getRecordedAt()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            row.createCell(4).setCellValue(record.getIpAddress());
            row.createCell(5).setCellValue(record.getBiometricSignature() != null ? "Yes" : "No");
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(createBoldFont(workbook));
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private Font createBoldFont(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        return font;
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 10; i++) { // Auto-size first 10 columns
            sheet.autoSizeColumn(i);
        }
    }

    private String sanitizeSheetName(String name) {
        // Excel sheet names have restrictions: max 31 chars, no :/\?*[]
        String sanitized = name.replaceAll("[:\\\\/\\?\\*\\[\\]]", "");
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }

    private LocalDate getPreviousMonday() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        
        // If today is Saturday, we want the Monday that just passed
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return monday;
        } else {
            // Otherwise, go back to previous week's Monday
            return monday.minusWeeks(1);
        }
    }

    /**
     * Send email with Excel attachment to faculty
     */
    private void sendEmailWithReport(UUID facultyId, byte[] excelReport, LocalDate weekStart, LocalDate weekEnd) {
        try {
            // Get faculty email from User repository
            User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found: " + facultyId));
            
            // Create email with Excel attachment
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            baos.write(excelReport);
            
            // Send via EmailService
            emailService.sendWeeklyReport(faculty.getEmail(), faculty.getName(), 
                baos.toByteArray(), weekStart, weekEnd);
            
            log.info("Weekly report email sent to faculty {} for week {} to {}", 
                       facultyId, weekStart, weekEnd);
        } catch (Exception e) {
            log.error("Failed to send weekly report email to faculty {}: {}", facultyId, e.getMessage());
        }
    }

    /**
     * Generate on-demand attendance report for date range
     */
    public byte[] generateOnDemandAttendanceReport(String startDate, String endDate) throws IOException {
        log.info("Generating on-demand attendance report from {} to {}", startDate, endDate);
        
        // Parse date parameters or use defaults
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : getPreviousMonday();
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : start.plusDays(6);
        
        Instant startTime = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        
        // Get all sessions for date range
        List<ClassroomSession> sessions = classroomSessionRepository
                .findByStartTimeBetween(startTime, endTime);
        
        // Generate Excel report
        return generateExcelReport(sessions, start, end);
    }

    /**
     * Generate weekly report for specific date range
     */
    public byte[] generateWeeklyReportForDateRange(LocalDate weekStart, LocalDate weekEnd) throws IOException {
        log.info("Generating weekly report for {} to {}", weekStart, weekEnd);
        
        Instant startTime = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = weekEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        
        // Get all sessions for week
        List<ClassroomSession> weeklySessions = classroomSessionRepository
                .findByStartTimeBetween(startTime, endTime);
        
        // Generate Excel report
        return generateExcelReport(weeklySessions, weekStart, weekEnd);
    }

    /**
     * Generate Excel report for a specific section with optional attendance threshold
     */
    public byte[] generateSectionAttendanceReport(UUID sectionId, Double threshold, LocalDate start, LocalDate end) 
            throws IOException {
        
        log.info("Generating section report for section: {}, threshold: {}, from {} to {}", 
                   sectionId, threshold, start, end);
        
        // 🔐 DLAC ENFORCEMENT for local Admins and Faculty
        checkDepartmentAccess(sectionId);

        Instant startTime = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        
        // 1. Get all sessions for this section in the date range
        List<ClassroomSession> sessions = classroomSessionRepository
                .findByStartTimeBetween(startTime, endTime)
                .stream()
                .filter(s -> s.getSection() != null && s.getSection().getId().equals(sectionId))
                .collect(Collectors.toList());
        
        if (sessions.isEmpty()) {
            return generateEmptyReport("No sessions found for the selected criteria.");
        }
        
        // 2. Get all students in this section
        List<User> studentList = userRepository.findStudentsBySectionId(sectionId);
        if (studentList == null || studentList.isEmpty()) {
            return generateEmptyReport("No students found currently enrolled in this section.");
        }

        Set<User> students = new HashSet<>(studentList);
        Map<UUID, Long> studentPresentCount = new HashMap<>();
        
        for (ClassroomSession session : sessions) {
            List<AttendanceRecord> records = attendanceRecordRepository
                    .findBySessionIdOrderByRecordedAtAsc(session.getId());
            
            for (AttendanceRecord record : records) {
                if (record.getStudent() != null && "PRESENT".equals(record.getStatus())) {
                    studentPresentCount.put(record.getStudent().getId(), 
                        studentPresentCount.getOrDefault(record.getStudent().getId(), 0L) + 1);
                }
            }
        }
        
        long totalSessions = sessions.size();
        
        // 3. Filter students based on threshold
        List<Map<String, Object>> reportRows = new ArrayList<>();
        for (User student : students) {
            long presentCount = studentPresentCount.getOrDefault(student.getId(), 0L);
            double percentage = totalSessions > 0 ? (presentCount * 100.0) / totalSessions : 0.0;
            
            if (threshold == null || percentage <= threshold) {
                Map<String, Object> row = new HashMap<>();
                row.put("name", student.getName() != null ? student.getName() : "Unknown Student");
                row.put("regNo", student.getRegistrationNumber() != null ? student.getRegistrationNumber() : "N/A");
                row.put("dept", resolveDepartmentName(student.getDepartment()));
                row.put("present", (Long) presentCount);
                row.put("absent", (Long) (totalSessions - presentCount));
                row.put("percentage", (Double) percentage);
                reportRows.add(row);
            }
        }
        
        // 4. Generate Excel
        return generateExcelFromData(reportRows, totalSessions, start, end);
    }

    /**
     * Generate simple student list for a section
     */
    public byte[] generateStudentListReport(UUID sectionId) throws IOException {
        log.info("Generating student list report for section: {}", sectionId);
        
        // 🔐 DLAC ENFORCEMENT
        checkDepartmentAccess(sectionId);
        
        List<User> students = userRepository.findStudentsBySectionId(sectionId);
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Student List");
            if (sheet instanceof SXSSFSheet sxSheet) sxSheet.trackAllColumnsForAutoSizing();
            
            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Student Name", "Reg Number", "Email", "Department", "Semester"};
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data Rows
            int rowNum = 1;
            for (User student : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(student.getName());
                row.createCell(1).setCellValue(student.getRegistrationNumber());
                row.createCell(2).setCellValue(student.getEmail());
                row.createCell(3).setCellValue(resolveDepartmentName(student.getDepartment()));
                row.createCell(4).setCellValue(student.getSemester() != null ? String.valueOf(student.getSemester()) : "N/A");
            }
            
            autoSizeColumns(sheet);
            workbook.write(bos);
            workbook.dispose();
            return bos.toByteArray();
        }
    }

    /**
     * 🏷️ RESOLVE UUID TO SECTION NAME
     */
    private String resolveSectionName(java.util.UUID sectionId) {
        if (sectionId == null) return "Unknown Section";
        return sectionRepository.findById(sectionId)
                .map(com.example.smartAttendence.entity.Section::getName)
                .orElse("Unknown Section");
    }

    /**
     * 🏷️ RESOLVE UUID TO DEPARTMENT NAME
     */
    private String resolveDepartmentName(String deptValue) {
        if (deptValue == null || deptValue.isBlank()) return "Unassigned";
        
        try {
            // 1. Try resolving if it's already a UUID
            java.util.UUID deptId = java.util.UUID.fromString(deptValue);
            
            // Try lookup in department repository directly
            return departmentRepository.findById(deptId)
                    .map(com.example.smartAttendence.entity.Department::getName)
                    .orElseGet(() -> 
                        // Fallback: Check if it's a section ID stored as department
                        sectionRepository.findById(deptId)
                            .map(s -> s.getDepartment().getName())
                            .orElse(deptValue) // Final fallback to raw value
                    );
        } catch (Exception e) {
            // Already a name or invalid UUID
            return deptValue;
        }
    }

    /**
     * 🔐 Robust Department Access Control (DLAC)
     * Throws SecurityException if the current user (ADMIN or FACULTY) attempts to access 
     * a section outside their department. SUPER_ADMINs are bypassed.
     */
    private void checkDepartmentAccess(UUID sectionId) {
        if (securityUtils.isSuperAdmin()) return;

        Optional<UUID> currentUserDeptId = securityUtils.getCurrentUserDepartmentId();
        
        if (currentUserDeptId.isPresent()) {
            com.example.smartAttendence.entity.Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));
            
            if (!section.getDepartment().getId().equals(currentUserDeptId.get())) {
                log.warn("🚨 DLAC VIOLATION: User attempted to access section {} outside department {}", 
                        sectionId, currentUserDeptId.get());
                throw new SecurityException("Access Denied: You can only access reports for your own department.");
            }
        } else if (securityUtils.isAdmin() || securityUtils.isFaculty()) {
            // If they are an admin/faculty but have no department assigned, deny access to everything for safety
            throw new SecurityException("Access Denied: No department assigned to your profile.");
        }
    }

    /**
     * 📦 GENERATE BULK ATTENDANCE REPORT (ZIP)
     * Creates a ZIP containing a summary report and individual section reports.
     */
    public byte[] generateBulkAttendanceReport(UUID deptId, UUID sectionId, LocalDate start, LocalDate end) throws IOException {
        // 🔐 DLAC ENFORCEMENT: Restrict non-Super-Admins to their own department
        if (!securityUtils.isSuperAdmin()) {
            Optional<UUID> userDeptId = securityUtils.getCurrentUserDepartmentId();
            if (userDeptId.isEmpty()) {
                throw new SecurityException("Access Denied: Your account must be assigned to a department to download bulk reports.");
            }

            // 1. If a specific department was requested, verify it matches the user's
            if (deptId != null && !deptId.equals(userDeptId.get())) {
                log.warn("🚨 SECURITY VIOLATION: Admin attempted to bulk download dept {} outside their dept {}", deptId, userDeptId.get());
                throw new SecurityException("Access Denied: You can only download reports for your assigned department.");
            }

            // 2. If no filters were provided, force the scope to the user's department
            if (deptId == null && sectionId == null) {
                deptId = userDeptId.get();
                log.info("ℹ️ Scoping bulk report to user's assigned department: {}", deptId);
            }

            // 3. If a specific section was requested, verify access (existing check)
            if (sectionId != null) {
                checkDepartmentAccess(sectionId);
            }
        }

        log.info("Generating bulk report: Dept={}, Sect={}, from {} to {}", deptId, sectionId, start, end);
        
        // 1. Determine the scope of sections
        List<com.example.smartAttendence.entity.Section> targetSections;
        if (sectionId != null) {
            targetSections = sectionRepository.findById(sectionId).map(List::of).orElse(Collections.emptyList());
        } else if (deptId != null) {
            targetSections = sectionRepository.findByDepartmentIdAndIsActiveTrue(deptId);
        } else {
            // All active sections for Super Admin
            targetSections = sectionRepository.findAll().stream()
                    .filter(com.example.smartAttendence.entity.Section::getIsActive)
                    .collect(Collectors.toList());
        }

        if (targetSections.isEmpty()) {
            return generateEmptyReport("No sections found for the specified filters.");
        }

        try (ByteArrayOutputStream zipBos = new ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(zipBos)) {
            
            List<Map<String, Object>> masterSummaryData = new ArrayList<>();

            // 2. Generate Detailed Section Reports and collect Summary Data
            for (com.example.smartAttendence.entity.Section section : targetSections) {
                try {
                    // Calculate percentages for summary
                    double sectionPercentage = calculateSectionAttendancePercentage(section.getId(), start, end);
                    
                    Map<String, Object> summaryRow = new HashMap<>();
                    summaryRow.put("deptName", section.getDepartment().getName());
                    summaryRow.put("sectionName", section.getName());
                    summaryRow.put("percentage", sectionPercentage);
                    masterSummaryData.add(summaryRow);

                    // Generate the actual section file
                    byte[] sectionExcel = generateSectionAttendanceReport(section.getId(), null, start, end);
                    
                    // Add to ZIP: "Dept Name (Section Name).xlsx"
                    String entryName = String.format("%s (%s).xlsx", 
                            section.getDepartment().getName(), section.getName());
                    
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
                    zos.putNextEntry(entry);
                    zos.write(sectionExcel);
                    zos.closeEntry();

                } catch (Exception e) {
                    log.error("Failed to generate report for section {}: {}", section.getName(), e.getMessage());
                }
            }

            // 3. Generate and Add Master Summary to ZIP
            byte[] summaryExcel = generateMasterSummaryExcel(masterSummaryData, start, end);
            java.util.zip.ZipEntry summaryEntry = new java.util.zip.ZipEntry("Attendance_Summary_Report.xlsx");
            zos.putNextEntry(summaryEntry);
            zos.write(summaryExcel);
            zos.closeEntry();

            zos.finish();
            return zipBos.toByteArray();
        }
    }

    private double calculateSectionAttendancePercentage(UUID sectionId, LocalDate start, LocalDate end) {
        Instant startTime = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        
        List<ClassroomSession> sessions = classroomSessionRepository.findByStartTimeBetween(startTime, endTime)
                .stream()
                .filter(s -> s.getSection().getId().equals(sectionId))
                .collect(Collectors.toList());

        if (sessions.isEmpty()) return 0.0;

        long totalPossible = 0;
        long totalPresent = 0;

        for (ClassroomSession session : sessions) {
            List<AttendanceRecord> records = attendanceRecordRepository.findBySessionIdOrderByRecordedAtAsc(session.getId());
            totalPossible += records.size();
            totalPresent += records.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
        }

        return totalPossible > 0 ? (totalPresent * 100.0 / totalPossible) : 0.0;
    }

    private byte[] generateMasterSummaryExcel(List<Map<String, Object>> summaryData, LocalDate start, LocalDate end) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Institution Summary");
            if (sheet instanceof SXSSFSheet sxSheet) sxSheet.trackAllColumnsForAutoSizing();
            
            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Department", "Section", "Attendance Percentage", "Report Period"};
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            String period = start.toString() + " to " + end.toString();
            
            // Data Rows
            int rowNum = 1;
            for (Map<String, Object> data : summaryData) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue((String) data.get("deptName"));
                row.createCell(1).setCellValue((String) data.get("sectionName"));
                
                Cell pctCell = row.createCell(2);
                pctCell.setCellValue((Double) data.get("percentage"));
                
                CellStyle pctStyle = workbook.createCellStyle();
                pctStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00\"%\""));
                pctCell.setCellStyle(pctStyle);
                
                row.createCell(3).setCellValue(period);
            }
            
            autoSizeColumns(sheet);
            workbook.write(bos);
            workbook.dispose();
            return bos.toByteArray();
        }
    }

    private byte[] generateEmptyReport(String message) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(5);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");
            sheet.createRow(0).createCell(0).setCellValue(message);
            workbook.write(bos);
            workbook.dispose();
            return bos.toByteArray();
        }
    }

    private byte[] generateExcelFromData(List<Map<String, Object>> rows, long totalSessions, LocalDate start, LocalDate end) 
            throws IOException {
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Attendance Report");
            if (sheet instanceof SXSSFSheet sxSheet) sxSheet.trackAllColumnsForAutoSizing();
            
            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Student Name", "Reg Number", "Department", "Sessions Present", "Sessions Absent", "Attendance %", "Total Possible"};
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data Rows
            int rowNum = 1;
            for (Map<String, Object> data : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue((String) data.get("name"));
                row.createCell(1).setCellValue((String) data.get("regNo"));
                row.createCell(2).setCellValue((String) data.get("dept"));
                row.createCell(3).setCellValue((Long) data.get("present"));
                row.createCell(4).setCellValue((Long) data.get("absent"));
                row.createCell(5).setCellValue(String.format("%.2f%%", (Double) data.get("percentage")));
                row.createCell(6).setCellValue(totalSessions);
            }
            
            // Auto-size columns
            autoSizeColumns(sheet);
            
            workbook.write(bos);
            workbook.dispose();
            return bos.toByteArray();
        }
    }
}
