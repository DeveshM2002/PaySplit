package com.splitwise.security;

import com.splitwise.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Adapter between our User entity and Spring Security's UserDetails interface.
 *
 * WHY do we need this?
 * Spring Security doesn't know about our User entity. It works with its own
 * UserDetails interface. This class bridges the gap — wraps our User and
 * implements UserDetails so Spring Security can use it.
 *
 * Alternative: Make User implement UserDetails directly.
 * WHY WE DON'T: That tightly couples our domain model to Spring Security.
 * If we ever swap security frameworks, we'd have to modify the User entity.
 * Keeping them separate follows the Single Responsibility Principle.
 *
 * WHY Collections.singletonList with ROLE_USER?
 * We don't have roles (admin/user) in this app — everyone is a regular user.
 * But Spring Security requires at least one authority. ROLE_USER is a convention.
 * If we add admin features later, we'd add ROLE_ADMIN here.
 */
@AllArgsConstructor
@Getter
public class UserPrincipal implements UserDetails {

    private Long id;
    private String name;
    private String email;
    private String password;

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
