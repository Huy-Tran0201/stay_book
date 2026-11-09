package com.staybook.staybook.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    java.util.Optional<PasswordResetToken> findByTokenHash(String tokenHash);}
