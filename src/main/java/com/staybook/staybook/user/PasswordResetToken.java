package com.staybook.staybook.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens", uniqueConstraints = @UniqueConstraint(name = "uk_reset_token_hash", columnNames = "token_hash"))
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "token_hash", nullable = false, length = 255) private String tokenHash;
    @Column(nullable = false) private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    protected PasswordResetToken() {}
}
