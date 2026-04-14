package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.enums.Role;
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

    @GetMapping("/check-admin")
    public ResponseEntity<Map<String, Object>> checkAdmin() {
        Map<String, Object> response = new HashMap<>();
        String adminEmail = "super.admin@smartattendence.com";
        
        var adminOpt = userV1Repository.findByEmailIgnoreCase(adminEmail);
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            response.put("status", "FOUND");
            response.put("email", admin.getEmail());
            response.put("role", admin.getRole());
            response.put("isCorrectRole", Role.SUPER_ADMIN.equals(admin.getRole()));
        } else {
            response.put("status", "NOT FOUND");
        }
        
        return ResponseEntity.ok(response);
    }
}
