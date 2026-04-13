package com.example.smartAttendence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_alerts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private com.example.smartAttendence.domain.User user;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "alert_message", nullable = false)
    private String alertMessage;

    @Column(nullable = false)
    private String severity = "MEDIUM";

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private com.example.smartAttendence.domain.User resolvedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "confidence")
    private Double confidence;
}
