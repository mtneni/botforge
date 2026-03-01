package org.legendstack.basebot.audit;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Immutable audit log entry. Tracks every significant action in BotForge
 * for compliance (SOC 2, HIPAA, internal audit).
 * <p>
 * Once created, audit records should never be updated or deleted.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_user", columnList = "userId"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_action", columnList = "action")
})
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(length = 64)
    private String username;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 64)
    private String sessionId;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(String userId, String username, String action,
            String detail, String ipAddress, String sessionId) {
        this.timestamp = Instant.now();
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
    }

    // Getters only — audit records are immutable
    public Long getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getSessionId() {
        return sessionId;
    }
}
