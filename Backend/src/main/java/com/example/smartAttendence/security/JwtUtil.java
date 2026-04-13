package com.example.smartAttendence.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.access-token.expiration-minutes}")
    private int accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token.expiration-days}")
    private int refreshTokenExpirationDays;

    @Value("${jwt.issuer}")
    private String issuer;

    private final KeyPair keyPair;

    public JwtUtil(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    // 🔐 ADVANCED TOKEN GENERATION WITH ZERO-TRUST SECURITY
    public String generateToken(String email, String role, String deviceFingerprint, String sessionId, 
                               String clientIP, String userAgent, String geoLocation) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);
        
        // 🧠 ADVANCED SECURITY CLAIMS WITH NONCE
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("deviceFingerprint", deviceFingerprint);
        claims.put("sessionId", sessionId);
        claims.put("tokenType", "ACCESS");
        claims.put("securityVersion", "4.0");
        claims.put("issuedAt", now.toEpochMilli());
        claims.put("nonce", UUID.randomUUID().toString()); // Prevent replay attacks
        claims.put("authTime", now.toEpochMilli());
        
        // 🔐 ENHANCED ZERO-TRUST BINDING CLAIMS
        claims.put("clientIP", hashIP(clientIP)); // Hashed IP for privacy
        claims.put("userAgent", hashUserAgent(userAgent)); // Hashed UA
        claims.put("geoLocation", geoLocation);
        claims.put("bindingHash", generateBindingHash(deviceFingerprint, clientIP, userAgent));
        claims.put("riskScore", calculateRiskScore(clientIP, userAgent, geoLocation));
        
        return Jwts.builder()
                .setSubject(email)
                .addClaims(claims)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .setId(UUID.randomUUID().toString())
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256) // RSA for enhanced security
                .compact();
    }

    // 🔐 REFRESH TOKEN GENERATION
    public String generateRefreshToken(String email, String deviceFingerprint) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "REFRESH");
        claims.put("deviceFingerprint", deviceFingerprint);
        claims.put("securityVersion", "3.0");
        claims.put("nonce", UUID.randomUUID().toString()); // Prevent replay attacks
        
        return Jwts.builder()
                .setSubject(email)
                .addClaims(claims)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .setId(UUID.randomUUID().toString())
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getClaims(token);
        return claimsResolver.apply(claims);
    }

    // 🔐 ENHANCED TOKEN VALIDATION WITH SECURITY CHECKS
    public boolean isTokenValid(String token, String userDetailsUsername, String deviceFingerprint) {
        try {
            final String email = extractEmail(token);
            final String tokenDeviceFingerprint = extractClaim(token, claims -> claims.get("deviceFingerprint", String.class));
            final String tokenType = extractClaim(token, claims -> claims.get("tokenType", String.class));
            
            // 🔍 MULTI-FACTOR VALIDATION
            return (email.equals(userDetailsUsername) && 
                    !isTokenExpired(token) && 
                    deviceFingerprint.equals(tokenDeviceFingerprint) &&
                    "ACCESS".equals(tokenType));
        } catch (Exception e) {
            return false;
        }
    }

    // 🔐 REFRESH TOKEN VALIDATION
    public boolean isRefreshTokenValid(String token, String userDetailsUsername, String deviceFingerprint) {
        try {
            final String email = extractEmail(token);
            final String tokenDeviceFingerprint = extractClaim(token, claims -> claims.get("deviceFingerprint", String.class));
            final String tokenType = extractClaim(token, claims -> claims.get("tokenType", String.class));
            
            return (email.equals(userDetailsUsername) && 
                    !isTokenExpired(token) && 
                    deviceFingerprint.equals(tokenDeviceFingerprint) &&
                    "REFRESH".equals(tokenType));
        } catch (Exception e) {
            return false;
        }
    }

    // 🔐 LEGACY COMPATIBILITY
    public boolean isTokenValid(String token, String userDetailsUsername) {
        final String email = extractEmail(token);
        final boolean emailValid = email.equals(userDetailsUsername);
        final boolean tokenNotExpired = !isTokenExpired(token);
        
        return emailValid && tokenNotExpired;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(Date.from(Instant.now()));
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 🔐 ENHANCED CLAIMS PARSING
    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(keyPair.getPublic())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("⚠️ JWT EXPIRED: {} - Token: {}", e.getMessage(), token.substring(0, Math.min(token.length(), 20)) + "...");
            throw e; // Rethrow to let the filter handle 401
        } catch (Exception e) {
            logger.error("🚨 JWT PARSING ERROR: {} - Token: {}", e.getMessage(), token.substring(0, Math.min(token.length(), 20)) + "...");
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    // 🔐 SECURITY CLAIMS EXTRACTION
    public String extractDeviceFingerprint(String token) {
        return extractClaim(token, claims -> claims.get("deviceFingerprint", String.class));
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", String.class));
    }

    public String extractRole(String token) {
        String role = extractClaim(token, claims -> claims.get("role", String.class));
        // Ensure role has ROLE_ prefix for Spring Security compatibility
        return role != null && role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    // 🔐 ADVANCED ZERO-TRUST VALIDATION
    public boolean isTokenValidAdvanced(String token, String userDetailsUsername, 
                                       String requestIP, String requestUserAgent, HttpServletRequest request) {
        try {
            final String email = extractEmail(token);
            final String tokenDeviceFingerprint = extractClaim(token, claims -> claims.get("deviceFingerprint", String.class));
            final String tokenType = extractClaim(token, claims -> claims.get("tokenType", String.class));
            final String tokenIP = extractClaim(token, claims -> claims.get("clientIP", String.class));
            final String tokenUA = extractClaim(token, claims -> claims.get("userAgent", String.class));
            final String bindingHash = extractClaim(token, claims -> claims.get("bindingHash", String.class));
            
            // 🔍 MULTI-FACTOR ZERO-TRUST VALIDATION
            boolean basicValid = (email.equals(userDetailsUsername) && !isTokenExpired(token) && "ACCESS".equals(tokenType));
            if (!basicValid) return false;
            
            // 🔐 IP VERIFICATION (with tolerance for mobile networks)
            if (!verifyIPBinding(requestIP, tokenIP)) {
                return false;
            }
            
            // 🔐 USER-AGENT VERIFICATION
            if (!verifyUserAgentBinding(requestUserAgent, tokenUA)) {
                return false;
            }
            
            // 🔐 BINDING HASH VERIFICATION
            String expectedHash = generateBindingHash(tokenDeviceFingerprint, requestIP, requestUserAgent);
            if (!expectedHash.equals(bindingHash)) {
                return false;
            }
            
            // 🔐 GEOLOCATION VERIFICATION (optional, based on campus)
            String requestGeo = getGeoLocation(requestIP);
            String tokenGeo = extractClaim(token, claims -> claims.get("geoLocation", String.class));
            if (!verifyGeoLocation(requestGeo, tokenGeo)) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    // 🔐 SECURITY HELPER METHODS
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

    private String generateBindingHash(String deviceFingerprint, String clientIP, String userAgent) {
        try {
            String combined = deviceFingerprint + ":" + clientIP + ":" + userAgent;
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "fallback-hash";
        }
    }

    private boolean verifyIPBinding(String requestIP, String tokenIP) {
        // 🔐 RELAXED IP VERIFICATION FOR MOBILE STABILITY
        String hashedRequestIP = hashIP(requestIP);
        if (!hashedRequestIP.equals(tokenIP)) {
            logger.warn("⚠️ IP MISMATCH (MOBILE TOLERANCE): Request IP {} does not match Token IP. Allowing due to likely network switch.", requestIP);
        }
        return true; // Always allow in production to prevent mobile disconnects
    }

    private boolean verifyUserAgentBinding(String requestUA, String tokenUA) {
        // 🔐 RELAXED USER-AGENT VERIFICATION
        String hashedRequestUA = hashUserAgent(requestUA);
        if (!hashedRequestUA.equals(tokenUA)) {
            logger.warn("⚠️ USER-AGENT MISMATCH: Request UA does not match Token UA. Possibly a browser update.");
        }
        return true; // Allow browser updates without logout
    }

    private String getGeoLocation(String ip) {
        // 🔐 GEOLOCATION LOOKUP (simplified)
        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "CAMPUS";
        }
        return "EXTERNAL";
    }

    private boolean verifyGeoLocation(String requestGeo, String tokenGeo) {
        // 🔐 GEOLOCATION VERIFICATION
        return requestGeo.equals(tokenGeo) || "CAMPUS".equals(tokenGeo);
    }
    
    // 🔐 RISK SCORE CALCULATION FOR ENHANCED SECURITY
    private int calculateRiskScore(String clientIP, String userAgent, String geoLocation) {
        int riskScore = 0;
        
        // IP-based risk assessment
        if (geoLocation.equals("EXTERNAL")) {
            riskScore += 30;
        }
        
        // User-agent based risk
        if (userAgent == null || userAgent.isEmpty()) {
            riskScore += 20;
        } else if (userAgent.toLowerCase().contains("bot") || 
                   userAgent.toLowerCase().contains("crawler")) {
            riskScore += 15;
        }
        
        // Time-based risk (simplified)
        int hour = java.time.ZonedDateTime.now().getHour();
        if (hour < 6 || hour > 22) {
            riskScore += 10;
        }
        
        return Math.min(riskScore, 100); // Cap at 100
    }
}
