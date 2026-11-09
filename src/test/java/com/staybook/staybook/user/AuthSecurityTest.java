package com.staybook.staybook.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.staybook.staybook.audit.AuditLogRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthSecurityTest.TestClockConfiguration.class)
class AuthSecurityTest {
    private static final String EMAIL = "auth-test@staybook.local";
    private static final String PASSWORD = "Staybook@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MutableClock clock;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new User("Guest Test", EMAIL, "0901234567",
            passwordEncoder.encode(PASSWORD), Role.GUEST, UserStatus.ACTIVE,
            clock.instant().atZone(clock.getZone()).toLocalDateTime()));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "GUEST")
    void guestCannotAccessStaffOrAdmin() throws Exception {
        mockMvc.perform(get("/staff/today")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reports")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "staff@staybook.local", roles = "STAFF")
    void staffCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/reports")).andExpect(status().isForbidden());
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", EMAIL)
                .param("password", PASSWORD))
            .andExpect(status().isForbidden());
    }

    @Test
    void fiveWrongPasswordsTemporarilyLockAccount() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/login")
                    .param("username", EMAIL)
                    .param("password", "wrong-password")
                    .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(attempt == 5 ? "/login?locked" : "/login?error"));
        }

        User locked = userRepository.findByEmail(EMAIL).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.TEMP_LOCKED, locked.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(5, locked.getFailedLoginCount());

        mockMvc.perform(post("/login")
                .param("username", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
            .andExpect(redirectedUrl("/login?locked"));

        clock.advanceSeconds(15 * 60);
        mockMvc.perform(post("/login")
                .param("username", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
            .andExpect(redirectedUrl("/"));

        User unlocked = userRepository.findByEmail(EMAIL).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.ACTIVE, unlocked.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0, unlocked.getFailedLoginCount());
    }

    @Test
    void successfulLoginClearsPreviousFailures() throws Exception {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setFailedLoginCount(2);
        userRepository.save(user);

        mockMvc.perform(post("/login")
                .param("username", EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
            .andExpect(redirectedUrl("/"));

        User reset = userRepository.findByEmail(EMAIL).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, reset.getFailedLoginCount());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfiguration {
        @Bean("testClock")
        @Primary
        MutableClock clock() {
            return new MutableClock(Instant.parse("2026-11-09T09:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }

    static final class MutableClock extends Clock {
        private Instant current;
        private final ZoneId zone;

        MutableClock(Instant current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        void advanceSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new MutableClock(current, zone); }
        @Override public Instant instant() { return current; }
    }
}
