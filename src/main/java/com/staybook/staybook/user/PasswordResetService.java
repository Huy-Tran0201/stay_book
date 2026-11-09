package com.staybook.staybook.user;

import com.staybook.staybook.notification.EmailService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private final UserRepository users; private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder; private final EmailService email; private final Clock clock;
    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder,
                                EmailService email, Clock clock) {
        this.users = users; this.tokens = tokens; this.encoder = encoder; this.email = email; this.clock = clock;
    }
    @Transactional
    public void request(String address) {
        users.findByEmail(UserService.normalize(address)).ifPresent(user -> {
            String raw = randomToken();
            tokens.save(new PasswordResetToken(user, hash(raw), LocalDateTime.now(clock).plusMinutes(30)));
            email.sendPasswordReset(user.getEmail(), raw);
        });
    }
    @Transactional
    public boolean reset(String raw, String password) {
        var token = tokens.findByTokenHash(hash(raw)).orElse(null);
        if (token == null || token.getUsedAt() != null || !LocalDateTime.now(clock).isBefore(token.getExpiresAt())) return false;
        token.getUser().setPasswordHash(encoder.encode(password)); token.markUsed(LocalDateTime.now(clock)); return true;
    }
    private static String randomToken() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    static String hash(String value) {
        try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
