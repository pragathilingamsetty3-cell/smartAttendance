import subprocess
import base64
import os

# The full clean content of AuthV1Controller.java (no emojis, with the fix)
controller_code = """package com.example.smartAttendence.controller.v1;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.dto.LoginRequest;
import com.example.smartAttendence.dto.v1.*;
import com.example.smartAttendence.entity.RefreshToken;
import com.example.smartAttendence.security.AdvancedInputValidator;
import com.example.smartAttendence.security.JwtUtil;
import com.example.smartAttendence.service.RefreshTokenService;
import com.example.smartAttendence.service.v1.UnifiedAuthService;
import com.example.smartAttendence.service.v1.AuthenticationService;
import com.example.smartAttendence.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthV1Controller {

    private final UnifiedAuthService unifiedAuthService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final SecurityUtils securityUtils;
    private final AdvancedInputValidator advancedInputValidator;
    private final AuthenticationService authenticationService;

    public AuthV1Controller(
            UnifiedAuthService unifiedAuthService,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService,
            SecurityUtils securityUtils,
            AdvancedInputValidator advancedInputValidator,
            AuthenticationService authenticationService) {
        this.unifiedAuthService = unifiedAuthService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.securityUtils = securityUtils;
        this.advancedInputValidator = advancedInputValidator;
        this.authenticationService = authenticationService;
    }

    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("[AUTH] Login attempt for email: {}", request.getEmail());
            AuthenticationService.LoginResult result = unifiedAuthService.login(request.getEmail(), request.getPassword(), extractDeviceId(httpRequest));
            
            if (result.status() == AuthenticationService.LoginStatus.REQUIRE_SETUP) {
                Map<String, Object> response = new HashMap<>();
                response.put("requiresFirstLoginSetup", true);
                response.put("message", "First login detected. Setup required.");
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", result.user().getId());
                userData.put("email", result.user().getEmail());
                userData.put("role", result.user().getRole());
                response.put("user", userData);
                return ResponseEntity.status(202).body(response);
            }

            String tokenFingerprint = generateDeviceFingerprint(httpRequest);
            String accessToken = jwtUtil.generateToken(result.user().getEmail(), result.user().getRole().toString(), tokenFingerprint, UUID.randomUUID().toString(), getClientIP(httpRequest), httpRequest.getHeader("User-Agent"), "EXTERNAL");
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(result.user());

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken.getToken());
            response.put("user", Map.of(
                "id", result.user().getId(),
                "email", result.user().getEmail(),
                "role", result.user().getRole(),
                "name", result.user().getName() != null ? result.user().getName() : ""
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[AUTH] Login Exception: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/complete-setup")
    public ResponseEntity<?> completeSetup(@Valid @RequestBody CompleteSetupRequest request, Authentication auth) {
        log.info("[AUTH] START: complete-setup request received");
        try {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            log.info("[AUTH] Calling authenticationService.completeSetup for user: {}", auth.getName());
            User user = authenticationService.completeSetup(request);
            if (user == null) {
                return ResponseEntity.status(500).body(Map.of("error", "Setup failed: Service returned invalid user state"));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Biometric setup completed successfully");
            response.put("secretKey", user.getSecretKey() != null ? user.getSecretKey() : "NOT_GENERATED");
            log.info("[AUTH] SUCCESS: complete-setup finished for {}", auth.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[AUTH] FATAL ERROR during setup: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Setup completion failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    @Transactional
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            String refreshTokenStr = request.get("refreshToken");
            RefreshToken token = refreshTokenService.findByToken(refreshTokenStr).orElseThrow(() -> new RuntimeException("Invalid refresh token"));
            refreshTokenService.verifyExpiration(token);
            String newToken = jwtUtil.generateToken(token.getUser().getEmail(), token.getUser().getRole().toString(), generateDeviceFingerprint(httpRequest), UUID.randomUUID().toString(), getClientIP(httpRequest), httpRequest.getHeader("User-Agent"), "EXTERNAL");
            return ResponseEntity.ok(Map.of("token", newToken, "refreshToken", refreshTokenStr));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            unifiedAuthService.getUserByEmail(auth.getName()).ifPresent(refreshTokenService::deleteByUser);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private String extractDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-ID");
        return deviceId != null ? deviceId : "UNKNOWN";
    }

    private String generateDeviceFingerprint(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return "SECURE_" + (ua != null ? ua.hashCode() : 0);
    }

    private String getClientIP(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0] : request.getRemoteAddr();
    }
}"""

def run_vm_cmd(cmd_str):
    full_cmd = [
        "az", "vm", "run-command", "invoke",
        "--resource-group", "SMART-ATTENDANCE-RG",
        "--name", "smart-attendance-server",
        "--command-id", "RunShellScript",
        "--scripts", cmd_str
    ]
    result = subprocess.run(full_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error: {result.stderr}")
    else:
        print(f"Success: {result.stdout}")

print("Chunking and uploading AuthV1Controller.java...")
b64_code = base64.b64encode(controller_code.encode()).decode()
chunk_size = 1000
chunks = [b64_code[i:i+chunk_size] for i in range(0, len(b64_code), chunk_size)]

# Clear file
run_vm_cmd("> /home/azureuser/auth_fix.b64")

# Append chunks
for i, chunk in enumerate(chunks):
    print(f"Uploading chunk {i+1}/{len(chunks)}...")
    run_vm_cmd(f"echo -n '{chunk}' >> /home/azureuser/auth_fix.b64")

# Decode and Build
print("Finalizing file and starting build...")
final_script = """
base64 -d /home/azureuser/auth_fix.b64 > /home/azureuser/Backend/src/main/java/com/example/smartAttendence/controller/v1/AuthV1Controller.java
sed -i '/@org.springframework.cache.annotation.Cacheable/d' /home/azureuser/Backend/src/main/java/com/example/smartAttendence/service/v1/StudentV1Service.java
export HOME=/home/azureuser
cd /home/azureuser/Backend
./mvnw clean package -DskipTests && bash /home/azureuser/final_start.sh
"""
run_vm_cmd(final_script)
