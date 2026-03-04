package com.smart_ecomernce_api.smart_ecomernce_api.security;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    @Getter
    private final Long id;
    @Getter
    private final String email;
    private final String password;
    private final Boolean isActive;
    private final Boolean isLocked;
    @Getter
    private final Long lastPasswordChangeEpoch;
    @Getter
    @Enumerated(value = EnumType.STRING)
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.isActive = user.getIsActive();
        this.isLocked = user.getIsLocked();
        this.lastPasswordChangeEpoch = user.getLastPasswordChange() != null
                ? user.getLastPasswordChange()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
                : null;
        this.role = user.getRole();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(user);
    }

    /**
     * Returns true if the account is locked (cannot authenticate).
     */
    public boolean isAccountLocked() {
        return Boolean.TRUE.equals(isLocked);
    }


    // ── UserDetails ────────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Delegates to the stored isLocked flag so Spring Security honours it. */
    @Override
    public boolean isAccountNonLocked() {
        return !Boolean.TRUE.equals(isLocked);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }
}