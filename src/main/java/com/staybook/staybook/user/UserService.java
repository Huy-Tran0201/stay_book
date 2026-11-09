package com.staybook.staybook.user;

import com.staybook.staybook.audit.AuditService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {
    public static final int MAX_FAILED_LOGINS = 5;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder,
                       AuditService auditService, Clock clock) {
        this.repository = repository; this.passwordEncoder = passwordEncoder;
        this.auditService = auditService; this.clock = clock;
    }

    @Transactional
    public User register(RegistrationForm form) {
        String email = normalize(form.getEmail());
        if (repository.findByEmail(email).isPresent()) throw new IllegalArgumentException("Email đã được sử dụng.");
        if (!form.getPassword().equals(form.getConfirmPassword()))
            throw new IllegalArgumentException("Mật khẩu nhập lại không khớp.");
        return repository.save(new User(form.getFullName().trim(), email, form.getPhone(),
            passwordEncoder.encode(form.getPassword()), Role.GUEST, UserStatus.ACTIVE,
            LocalDateTime.now(clock)));
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) {
        User user = repository.findByEmail(normalize(username))
            .orElseThrow(() -> new UsernameNotFoundException("Email hoặc mật khẩu không chính xác."));
        if (user.getStatus() == UserStatus.LOCKED)
            throw new LockedException("Tài khoản đã bị khóa. Vui lòng liên hệ cơ sở.");
        if (user.getStatus() == UserStatus.TEMP_LOCKED) {
            if (user.getLockedUntil() != null && LocalDateTime.now(clock).isBefore(user.getLockedUntil()))
                throw new LockedException("Tài khoản tạm khóa.");
            user.resetLoginFailures();
        }
        return UserDetailsImpl.from(user);
    }

    @Transactional
    public void recordLoginFailure(String email) {
        repository.findByEmail(normalize(email)).ifPresent(user -> {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            if (failures >= MAX_FAILED_LOGINS) {
                user.setStatus(UserStatus.TEMP_LOCKED);
                user.setLockedUntil(LocalDateTime.now(clock).plusMinutes(15));
                auditService.record(user, "ACCOUNT_TEMP_LOCKED", "Khóa tạm sau 5 lần đăng nhập sai");
            } else {
                auditService.record(user, "LOGIN_FAILED", "Đăng nhập sai lần " + failures);
            }
        });
    }

    @Transactional
    public void recordLoginSuccess(String email) {
        repository.findByEmail(normalize(email)).ifPresent(user -> {
            user.resetLoginFailures();
            repository.save(user);
        });
    }

    public User findByEmail(String email) {
        return repository.findByEmail(normalize(email)).orElseThrow();
    }

    public boolean isLocked(String email) {
        return repository.findByEmail(normalize(email))
            .map(user -> user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.TEMP_LOCKED)
            .orElse(false);
    }

    public static String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }

    @Transactional
    public void updateProfile(String email, ProfileForm form) {
        User user = repository.findByEmail(normalize(email)).orElseThrow();
        user.setFullName(form.getFullName().trim());
        user.setPhone(form.getPhone());
        if (form.getNewPassword() != null && !form.getNewPassword().isBlank()) {
            if (form.getCurrentPassword() == null ||
                !passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash()))
                throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
            user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        }
    }
}
