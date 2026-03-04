package com.smart_ecomernce_api.smart_ecomernce_api.modules.auth;

import com.smart_ecomernce_api.smart_ecomernce_api.security.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class AuthSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Truly public — no token needed at all
                .requestMatchers("/v1/auth/register", "/v1/auth/login").permitAll()

                // Logout should be accessible without authentication
                // The user is already trying to log out, and we use the refreshToken from request body
                .requestMatchers("/v1/auth/logout").permitAll()

                // Requires a valid token — user must be logged in
                .requestMatchers(
                        "/v1/auth/refresh",
                        "/v1/auth/me",
                        "/v1/auth/password/change"
                ).authenticated()

                // Admin only
                .requestMatchers(
                        "/v1/auth/account/lock",
                        "/v1/auth/account/unlock",
                        "/v1/auth/security/**"
                ).hasRole("ADMIN");
    }
}
