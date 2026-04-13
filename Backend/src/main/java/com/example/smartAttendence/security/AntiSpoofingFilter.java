package com.example.smartAttendence.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AntiSpoofingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AntiSpoofingFilter.class);

    private static final String MOCKED_HEADER = "X-Device-Mocked";
    private static final Set<String> ALLOWED_CAMPUS_IPS = Set.of(
            "192.168.1.100",
            "127.0.0.1",
            "0:0:0:0:0:0:0:1"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/api/v1/attendance/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String mockedHeaderValue = request.getHeader(MOCKED_HEADER);
        if (mockedHeaderValue != null && mockedHeaderValue.equalsIgnoreCase("true")) {
            throw new AccessDeniedException("Mocked device detected (X-Device-Mocked=true)");
        }

        String remoteAddr = request.getRemoteAddr();
        // 🔐 SECURITY: Log IP securely without exposing sensitive details
        logger.debug("Incoming request from IP: {}", remoteAddr);

        String normalizedIp = (remoteAddr == null) ? null : remoteAddr.trim().toLowerCase();
        boolean allowedCampusIp = normalizedIp != null && ALLOWED_CAMPUS_IPS.contains(normalizedIp);

        if (!allowedCampusIp) {
            // Treat as cellular / off-campus data, let deeper security layers handle it.
            request.setAttribute("isCellularData", Boolean.TRUE);
        }

        filterChain.doFilter(request, response);
    }
}

