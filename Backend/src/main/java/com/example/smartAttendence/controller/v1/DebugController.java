package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);
    
    @Autowired
    private UserV1Repository userV1Repository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/test-passwords")
    public ResponseEntity<Map<String, Object>> testPasswords() {
        Map<String, Object> response = new HashMap<>();
        
        // Test faculty user
        String facultyEmail = "v1.test.professor@smart.local";
        String facultyTestPassword = "SecureTempPass123@";
        
        var facultyOpt = userV1Repository.findByEmail(facultyEmail);
        if (facultyOpt.isPresent()) {
            User faculty = facultyOpt.get();
            String storedPassword = faculty.getPassword();
            
            Map<String, Object> facultyInfo = new HashMap<>();
            facultyInfo.put("email", facultyEmail);
            facultyInfo.put("role", faculty.getRole());
            facultyInfo.put("passwordFormat", storedPassword.startsWith("$2") ? "BCrypt" : "Plain text");
            facultyInfo.put("storedPassword", storedPassword);
            facultyInfo.put("testPassword", facultyTestPassword);
            facultyInfo.put("passwordMatches", passwordEncoder.matches(facultyTestPassword, storedPassword));
            
            response.put("faculty", facultyInfo);
        } else {
            response.put("faculty", "NOT FOUND");
        }
        
        // Test admin user
        String adminEmail = "super.admin@smartattendence.com";
        String adminTestPassword = "Mani_Smart_Attendance_2026";
        
        var adminOpt = userV1Repository.findByEmail(adminEmail);
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            String storedPassword = admin.getPassword();
            
            Map<String, Object> adminInfo = new HashMap<>();
            adminInfo.put("email", adminEmail);
            adminInfo.put("role", admin.getRole());
            adminInfo.put("passwordFormat", storedPassword.startsWith("$2") ? "BCrypt" : "Plain text");
            adminInfo.put("storedPassword", storedPassword);
            adminInfo.put("testPassword", adminTestPassword);
            adminInfo.put("passwordMatches", passwordEncoder.matches(adminTestPassword, storedPassword));
            
            response.put("admin", adminInfo);
        } else {
            response.put("admin", "NOT FOUND");
        }
        
        return ResponseEntity.ok(response);
    }
}
