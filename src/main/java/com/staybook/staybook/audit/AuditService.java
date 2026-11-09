package com.staybook.staybook.audit;

import com.staybook.staybook.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void record(User user, String action, String detail) {
        repository.save(new AuditLog(user, action, "USER", user == null ? null : user.getId(),
            detail, LocalDateTime.now(clock)));
    }
}
