package com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.entity;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_sessions", indexes = {
        @Index(name = "idx_auth_user", columnList = "user_id"),
        @Index(name = "idx_auth_token", columnList = "refresh_token", unique = true),
        @Index(name = "idx_auth_active", columnList = "is_active"),
        @Index(name = "idx_auth_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Auth extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 500)
    private String refreshToken;

    @Column(name = "access_token", length = 500)
    private String accessToken;

    @Column(name = "token_type", length = 20)
    @Builder.Default
    private String tokenType = "Bearer";

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "logged_out_at")
    private LocalDateTime loggedOutAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }



}
