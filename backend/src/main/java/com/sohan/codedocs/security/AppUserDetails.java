package com.sohan.codedocs.security;

import com.sohan.codedocs.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Wraps our User entity as the Spring Security principal. This is what
 * carries the user's id through the session: UserDetailsService is only
 * invoked once, at login, and the resulting Authentication (principal
 * included) is what actually gets serialized into the HTTP session — so
 * putting id here means every later request gets it for free, no repeated
 * lookup by email.
 */
public class AppUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
    }

    public UUID id() {
        return id;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
