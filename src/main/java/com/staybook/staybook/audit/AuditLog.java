package com.staybook.staybook.audit;

import com.staybook.staybook.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = @Index(name = "ix_audit_log_at", columnList = "log_at"))
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 100) private String action;
    @Column(nullable = false, length = 100) private String targetType;
    private Long targetId;
    @Column(length = 2000) private String detail;
    @Column(name = "log_at", nullable = false) private LocalDateTime at;
    protected AuditLog() {}
}
