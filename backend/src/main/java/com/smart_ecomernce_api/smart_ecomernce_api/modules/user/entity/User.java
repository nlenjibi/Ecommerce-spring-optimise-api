package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity;


import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.entity.Auth;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_id", columnList = "id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    @NotBlank
    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @NotBlank
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;


    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "phone_verified")
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    // ========================================
    // Security
    // ========================================

    @Column(name = "email_verification_token", length = 100)
    private String emailVerificationToken;

    @Column(name = "email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;

    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "github_id")
    private String githubId;

    @Column(name = "facebook_id")
    private String facebookId;

    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "oauth_access_token", length = 500)
    private String oauthAccessToken;

    @Column(name = "profileImageUrl", length = 500)
    private String profileImageUrl;

    @Override
    public String toString() {
        return getFullName() + "(" +

                "email = " + email + ")";
    }
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean getIsLocked() {
        return Boolean.TRUE.equals(isLocked);
    }
}
