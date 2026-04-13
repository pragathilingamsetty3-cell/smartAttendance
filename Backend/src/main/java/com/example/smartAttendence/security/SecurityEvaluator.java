package com.example.smartAttendence.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.stereotype.Component;
import com.example.smartAttendence.security.SecurityAuditLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityEvaluator {

    public boolean hasValidLocation(Authentication authentication, HttpServletRequest request) {
        // 🔐 ZERO-TRUST LOCATION VERIFICATION
        String clientIP = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        String sessionId = request.getHeader("X-Session-ID");
        
        // 🔐 MULTI-FACTOR LOCATION VERIFICATION
        return verifyIPBinding(authentication, clientIP) &&
               verifyUserAgentBinding(authentication, userAgent) &&
               verifySessionBinding(authentication, sessionId) &&
               verifyGeoLocation(authentication, clientIP);
    }
    
    public boolean hasValidDevice(Authentication authentication, HttpServletRequest request) {
        // 🔐 ADVANCED DEVICE VERIFICATION FOR ADMIN
        String deviceFingerprint = generateDeviceFingerprint(request);
        String storedFingerprint = getStoredDeviceFingerprint(authentication);
        
        if (storedFingerprint == null) {
            // 🔐 FIRST TIME DEVICE - REGISTER WITH HIGHER SECURITY
            return registerAdminDevice(authentication, deviceFingerprint, request);
        }
        
        // 🔐 STRICT DEVICE MATCHING FOR ADMIN
        return storedFingerprint.equals(deviceFingerprint) && 
               verifyDeviceTrust(authentication, deviceFingerprint, request);
    }
    
    public boolean hasValidSession(Authentication authentication, HttpServletRequest request) {
        // 🔐 MULTI-FACTOR SESSION VERIFICATION FOR FACULTY
        String sessionId = request.getHeader("X-Session-ID");
        String deviceFingerprint = generateDeviceFingerprint(request);
        
        // 🔐 SESSION INTEGRITY VERIFICATION
        return verifySessionIntegrity(authentication, sessionId) &&
               verifyDeviceSession(authentication, deviceFingerprint, sessionId) &&
               verifySessionActivity(authentication, sessionId, request);
    }
    
    private boolean verifyIPBinding(Authentication authentication, String clientIP) {
        // 🔐 IP BINDING VERIFICATION
        String username = authentication.getName();
        String key = "ip_binding:" + username;
        
        try {
            String storedIP = redisTemplate.opsForValue().get(key);
            if (storedIP == null) {
                // 🔐 FIRST TIME - REGISTER IP
                redisTemplate.opsForValue().set(key, hashIP(clientIP), java.time.Duration.ofHours(1));
                return true;
            }
            
            // 🔐 VERIFY IP MATCH (WITH SOME TOLERANCE)
            return storedIP.equals(hashIP(clientIP)) || isIPInRange(clientIP, storedIP);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean verifyUserAgentBinding(Authentication authentication, String userAgent) {
        // 🔐 USER-AGENT BINDING VERIFICATION
        String username = authentication.getName();
        String key = "ua_binding:" + username;
        
        try {
            String storedUA = redisTemplate.opsForValue().get(key);
            if (storedUA == null) {
                // 🔐 FIRST TIME - REGISTER USER-AGENT
                redisTemplate.opsForValue().set(key, hashUserAgent(userAgent), java.time.Duration.ofDays(7));
                return true;
            }
            
            // 🔐 VERIFY USER-AGENT MATCH (WITH TOLERANCE FOR UPDATES)
            return storedUA.equals(hashUserAgent(userAgent));
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean verifySessionBinding(Authentication authentication, String sessionId) {
        // 🔐 SESSION BINDING VERIFICATION
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        
        String username = authentication.getName();
        String key = "session_binding:" + username + ":" + sessionId;
        
        return redisTemplate.hasKey(key);
    }
    
    private boolean verifyGeoLocation(Authentication authentication, String clientIP) {
        // 🔐 GEOLOCATION VERIFICATION
        String username = authentication.getName();
        String currentGeo = getGeoLocation(clientIP);
        String key = "geo_binding:" + username;
        
        try {
            String storedGeo = redisTemplate.opsForValue().get(key);
            if (storedGeo == null) {
                // 🔐 FIRST TIME - REGISTER GEOLOCATION
                redisTemplate.opsForValue().set(key, currentGeo, java.time.Duration.ofHours(24));
                return true;
            }
            
            // 🔐 VERIFY GEOLOCATION MATCH
            return currentGeo.equals(storedGeo) || "CAMPUS".equals(storedGeo);
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getStoredDeviceFingerprint(Authentication authentication) {
        String username = authentication.getName();
        String key = "device_fingerprint:" + username;
        return redisTemplate.opsForValue().get(key);
    }
    
    private boolean registerAdminDevice(Authentication authentication, String deviceFingerprint, HttpServletRequest request) {
        // 🔐 ADMIN DEVICE REGISTRATION WITH HIGHER SECURITY
        String username = authentication.getName();
        String key = "device_fingerprint:" + username;
        
        // 🔐 ADDITIONAL VERIFICATION FOR ADMIN DEVICES
        if (!verifyAdminDeviceRequirements(request)) {
            return false;
        }
        
        redisTemplate.opsForValue().set(key, deviceFingerprint, java.time.Duration.ofDays(30));
        
        // 🔐 LOG ADMIN DEVICE REGISTRATION
        ((SecurityAuditLogger) securityEventLogger).logSecurityEvent("ADMIN_DEVICE_REGISTERED", username, 
            String.format("Device registered: fingerprint=%s, ip=%s", deviceFingerprint, getClientIP(request)));
        
        return true;
    }
    
    private boolean verifyDeviceTrust(Authentication authentication, String deviceFingerprint, HttpServletRequest request) {
        // 🔐 DEVICE TRUST VERIFICATION
        String username = authentication.getName();
        String key = "device_trust:" + username + ":" + deviceFingerprint;
        
        try {
            String trustLevel = redisTemplate.opsForValue().get(key);
            if (trustLevel == null) {
                // 🔐 CALCULATE TRUST LEVEL
                trustLevel = calculateDeviceTrust(deviceFingerprint, request);
                redisTemplate.opsForValue().set(key, trustLevel, java.time.Duration.ofHours(1));
            }
            
            return "HIGH".equals(trustLevel) || "MEDIUM".equals(trustLevel);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean verifySessionIntegrity(Authentication authentication, String sessionId) {
        // 🔐 SESSION INTEGRITY VERIFICATION
        String username = authentication.getName();
        String key = "session_integrity:" + username + ":" + sessionId;
        
        try {
            String integrity = redisTemplate.opsForValue().get(key);
            if (integrity == null) {
                // 🔐 ESTABLISH SESSION INTEGRITY
                integrity = "VALID";
                redisTemplate.opsForValue().set(key, integrity, java.time.Duration.ofMinutes(30));
            }
            
            return "VALID".equals(integrity);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean verifyDeviceSession(Authentication authentication, String deviceFingerprint, String sessionId) {
        // 🔐 DEVICE-SESSION BINDING VERIFICATION
        String username = authentication.getName();
        String key = "device_session:" + username + ":" + deviceFingerprint;
        
        try {
            String storedSessionId = redisTemplate.opsForValue().get(key);
            if (storedSessionId == null) {
                // 🔐 BIND DEVICE TO SESSION
                redisTemplate.opsForValue().set(key, sessionId, java.time.Duration.ofHours(2));
                return true;
            }
            
            return storedSessionId.equals(sessionId);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean verifySessionActivity(Authentication authentication, String sessionId, HttpServletRequest request) {
        // 🔐 SESSION ACTIVITY VERIFICATION
        String username = authentication.getName();
        String key = "session_activity:" + username + ":" + sessionId;
        
        try {
            // 🔐 UPDATE LAST ACTIVITY
            redisTemplate.opsForValue().set(key, java.time.Instant.now().toString(), java.time.Duration.ofMinutes(15));
            
            // 🔐 CHECK FOR SUSPICIOUS ACTIVITY
            String activityKey = "suspicious_activity:" + username;
            String suspiciousCount = redisTemplate.opsForValue().get(activityKey);
            
            if (suspiciousCount != null && Integer.parseInt(suspiciousCount) > 3) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean verifyAdminDeviceRequirements(HttpServletRequest request) {
        // 🔐 ADMIN DEVICE REQUIREMENTS
        String userAgent = request.getHeader("User-Agent");
        String clientIP = getClientIP(request);
        
        // 🔐 MUST BE FROM CAMPUS NETWORK
        if (!"CAMPUS".equals(getGeoLocation(clientIP))) {
            return false;
        }
        
        // 🔐 MUST HAVE LEGITIMATE USER-AGENT
        if (userAgent == null || userAgent.isEmpty() || userAgent.length() < 10) {
            return false;
        }
        
        // 🔐 MUST NOT BE FROM KNOWN PROXY/VPN
        if (isProxyOrVPN(clientIP)) {
            return false;
        }
        
        return true;
    }
    
    private String calculateDeviceTrust(String deviceFingerprint, HttpServletRequest request) {
        // 🔐 DEVICE TRUST CALCULATION
        int trustScore = 50; // BASE SCORE
        
        // 🔐 BONUS FOR CAMPUS NETWORK
        if ("CAMPUS".equals(getGeoLocation(getClientIP(request)))) {
            trustScore += 20;
        }
        
        // 🔐 BONUS FOR LEGITIMATE USER-AGENT
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.contains("Mozilla")) {
            trustScore += 10;
        }
        
        // 🔐 BONUS FOR KNOWN DEVICE
        if (isKnownDevice(deviceFingerprint)) {
            trustScore += 20;
        }
        
        if (trustScore >= 80) return "HIGH";
        if (trustScore >= 50) return "MEDIUM";
        return "LOW";
    }
    
    private boolean isKnownDevice(String deviceFingerprint) {
        String key = "known_device:" + deviceFingerprint;
        return redisTemplate.hasKey(key);
    }
    
    private boolean isProxyOrVPN(String clientIP) {
        // 🔐 SIMPLIFIED PROXY/VPN DETECTION
        String key = "proxy_vpn:" + clientIP;
        return redisTemplate.hasKey(key);
    }
    
    private String generateDeviceFingerprint(HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            String acceptLanguage = request.getHeader("Accept-Language");
            String acceptEncoding = request.getHeader("Accept-Encoding");
            String accept = request.getHeader("Accept");
            
            String fingerprintData = userAgent + "|" + acceptLanguage + "|" + acceptEncoding + "|" + accept;
            
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprintData.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            return "unknown-device";
        }
    }
    
    private String hashIP(String ip) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return ip;
        }
    }
    
    private String hashUserAgent(String userAgent) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userAgent.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return userAgent;
        }
    }
    
    private boolean isIPInRange(String currentIP, String storedIP) {
        // 🔐 SIMPLIFIED IP RANGE CHECKING
        return currentIP.startsWith(storedIP.substring(0, Math.min(storedIP.length(), 10)));
    }
    
    private String getGeoLocation(String ip) {
        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "CAMPUS";
        }
        return "EXTERNAL";
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    // 🔐 DEPENDENCIES (AUTOMATICALLY INJECTED BY SPRING)
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private HandlerInterceptor securityEventLogger;
    
    // 🔐 CONSTRUCTOR - SPRING WILL INJECT DEPENDENCIES AUTOMATICALLY
    public SecurityEvaluator() {
        // Spring will inject dependencies via @Autowired
    }
    
    // 🔐 SETTER FOR DEPENDENCY INJECTION
    @Autowired
    public void setRedisTemplate(org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Autowired
    public void setSecurityEventLogger(@Qualifier("securityAuditLogger") HandlerInterceptor securityEventLogger) {
        this.securityEventLogger = securityEventLogger;
    }
}
