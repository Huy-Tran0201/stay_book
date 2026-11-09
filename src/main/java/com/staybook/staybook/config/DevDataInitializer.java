package com.staybook.staybook.config;

import com.staybook.staybook.user.Role;
import com.staybook.staybook.user.User;
import com.staybook.staybook.user.UserRepository;
import com.staybook.staybook.user.UserStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevDataInitializer {
    @Bean
    CommandLineRunner seedDevUsers(UserRepository repository, PasswordEncoder encoder, Clock clock) {
        return args -> {
            List<String> emails = List.of("admin@staybook.local", "staff1@staybook.local",
                "staff2@staybook.local", "guest1@staybook.local", "guest2@staybook.local");
            for (String email : emails) {
                if (repository.findByEmail(email).isEmpty()) {
                    Role role = email.startsWith("admin") ? Role.ADMIN :
                        email.startsWith("staff") ? Role.STAFF : Role.GUEST;
                    repository.save(new User(email.substring(0, email.indexOf('@')), email,
                        "0900000000", encoder.encode("Staybook@123"), role, UserStatus.ACTIVE,
                        LocalDateTime.now(clock)));
                }
            }
        };
    }
}
