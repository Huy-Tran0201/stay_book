package com.staybook.staybook.user;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

final class UserDetailsImpl implements UserDetails {
    private final User user;
    private UserDetailsImpl(User user) { this.user = user; }
    static UserDetailsImpl from(User user) { return new UserDetailsImpl(user); }
    public List<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
    public String getPassword() { return user.getPasswordHash(); }
    public String getUsername() { return user.getEmail(); }
    public boolean isAccountNonExpired() { return true; }
    public boolean isAccountNonLocked() { return user.getStatus() == UserStatus.ACTIVE; }
    public boolean isCredentialsNonExpired() { return true; }
    public boolean isEnabled() { return true; }
}
