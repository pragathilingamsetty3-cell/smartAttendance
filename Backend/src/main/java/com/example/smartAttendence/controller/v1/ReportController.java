package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.lib.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACULTY')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Generate and download Excel attendance report
     * GET /api/v1/reports/attendance/excel
     */
    @GetMapping("/attendance/excel")
    public ResponseEntity<byte[]> downloadAttendanceReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            // Generate filename with current date
            String fileName = "attendance_report_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            
            // Generate Excel report using existing service
            // Note: ReportService.generateWeeklyReports() is scheduled, 
            // so we'll create a simple wrapper for on-demand generation
            byte[] excelData = reportService.generateOnDemandAttendanceReport(startDate, endDate);
            
            // Set headers for Excel download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (Exception e) {
            // Return error response
            return ResponseEntity.internalServerError()
                    .body(("Error generating Excel report: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Generate weekly Excel report for specific date range
     * GET /api/v1/reports/attendance/weekly
     */
    @GetMapping("/attendance/weekly")
    public ResponseEntity<byte[]> downloadWeeklyReport(
            @RequestParam String weekStart,
            @RequestParam String weekEnd) {
        
        try {
            // Validate date parameters
            LocalDate startDate = LocalDate.parse(weekStart);
            LocalDate endDate = LocalDate.parse(weekEnd);
            
            String fileName = "weekly_attendance_report_" + 
                startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_to_" +
                endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            
            // Generate weekly Excel report
            byte[] excelData = reportService.generateWeeklyReportForDateRange(startDate, endDate);
            
            // Set headers for Excel download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(("Error generating weekly Excel report: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Generate and download Excel report for a specific section
     * GET /api/v1/reports/attendance/section?sectionId=...&threshold=...&startDate=...&endDate=...
     */
    @GetMapping("/attendance/section")
    public ResponseEntity<byte[]> downloadSectionReport(
            @RequestParam java.util.UUID sectionId,
            @RequestParam(required = false) Double threshold,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            // Parse dates or use default range (last 30 days)
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            String fileName = "section_attendance_" + 
                start.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_to_" +
                end.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            
            byte[] excelData = reportService.generateSectionAttendanceReport(sectionId, threshold, start, end);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(("Error generating section report: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Generate and download Excel student list for a specific section
     * GET /api/v1/reports/attendance/section/{sectionId}/students
     */
    @GetMapping("/attendance/section/{sectionId}/students")
    public ResponseEntity<byte[]> downloadStudentList(@PathVariable java.util.UUID sectionId) {
        try {
            String fileName = "student_list_section_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            
            byte[] excelData = reportService.generateStudentListReport(sectionId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(("Error generating student list: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Bulk download attendance reports in a ZIP package
     * GET /api/v1/reports/attendance/bulk?deptId=...&sectionId=...&startDate=...&endDate=...
     */
    @GetMapping("/attendance/bulk")
    public ResponseEntity<byte[]> downloadBulkReport(
            @RequestParam(required = false) java.util.UUID deptId,
            @RequestParam(required = false) java.util.UUID sectionId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            
            String fileName = "Bulk_Attendance_Report_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".zip";
            
            byte[] zipData = reportService.generateBulkAttendanceReport(deptId, sectionId, start, end);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(zipData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(("Error generating bulk report: " + e.getMessage()).getBytes());
        }
    }
}
