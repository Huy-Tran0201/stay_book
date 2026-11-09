package com.staybook.staybook.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_user_email", columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String fullName;
    @Column(nullable = false, length = 160) private String email;
    @Column(nullable = false, length = 20) private String phone;
    @Column(nullable = false, length = 255) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserStatus status;
    @Column(nullable = false) private int failedLoginCount;
    private LocalDateTime lockedUntil;
    @Column(nullable = false) private LocalDateTime createdAt;

    protected User() {}
    public User(String fullName, String email, String phone, String passwordHash, Role role, UserStatus status, LocalDateTime createdAt) {
        this.fullName = fullName; this.email = email; this.phone = phone; this.passwordHash = passwordHash;
        this.role = role; this.status = status; this.createdAt = createdAt;
    }
    public void resetLoginFailures() {
        failedLoginCount = 0;
        lockedUntil = null;
        if (status == UserStatus.TEMP_LOCKED) status = UserStatus.ACTIVE;
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; } public void setFullName(String v) { fullName = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getPasswordHash() { return passwordHash; } public void setPasswordHash(String v) { passwordHash = v; }
    public Role getRole() { return role; } public void setRole(Role v) { role = v; }
    public UserStatus getStatus() { return status; } public void setStatus(UserStatus v) { status = v; }
    public int getFailedLoginCount() { return failedLoginCount; } public void setFailedLoginCount(int v) { failedLoginCount = v; }
    public LocalDateTime getLockedUntil() { return lockedUntil; } public void setLockedUntil(LocalDateTime v) { lockedUntil = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
