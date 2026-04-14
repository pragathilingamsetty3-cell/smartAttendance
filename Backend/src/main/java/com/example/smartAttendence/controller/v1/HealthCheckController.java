package com.example.smartAttendence.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Smart Attendance Engine");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return status;
    }
    
    @GetMapping("/")
    public String home() {
        return "Smart Attendance Engine is running!";
    }
}
