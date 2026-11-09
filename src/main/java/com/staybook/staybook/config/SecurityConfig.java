package com.staybook.staybook.config;

import com.staybook.staybook.user.UserService;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.savedrequest.*;

@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserService userService) throws Exception {
        http.userDetailsService(userService)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/rooms", "/room-types/**", "/login", "/register",
                    "/forgot-password", "/reset-password", "/error", "/error/**",
                    "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").successHandler(successHandler(userService))
                .failureHandler(failureHandler(userService)).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            .exceptionHandling(exception -> exception.accessDeniedPage("/error/403"))
            .sessionManagement(session -> session.sessionFixation().migrateSession());
        return http.build();
    }

    private AuthenticationSuccessHandler successHandler(UserService userService) {
        return (request, response, authentication) -> {
            userService.recordLoginSuccess(authentication.getName());
            String saved = request.getSession(false) == null ? null :
                new HttpSessionRequestCache().getRequest(request, response) == null ? null :
                new HttpSessionRequestCache().getRequest(request, response).getRedirectUrl();
            if (isInternal(saved)) { response.sendRedirect(saved); return; }
            String target = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))
                ? "/" : "/staff/today";
            response.sendRedirect(target);
        };
    }

    private AuthenticationFailureHandler failureHandler(UserService userService) {
        return (request, response, exception) -> {
            userService.recordLoginFailure(request.getParameter("username"));
            String code = exception instanceof LockedException
                || userService.isLocked(request.getParameter("username")) ? "locked" : "error";
            response.sendRedirect("/login?" + code);
        };
    }

    private static boolean isInternal(String url) {
        return url != null && url.startsWith("/") && !url.startsWith("//") && !url.contains("\\");
    }
}
