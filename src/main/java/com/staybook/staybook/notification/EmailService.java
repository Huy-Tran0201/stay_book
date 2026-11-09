package com.staybook.staybook.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    public void sendPasswordReset(String email, String token) {
        log.info("Password reset email for {}: token={}", email, token);
    }
}
