package com.smart_ecomernce_api.smart_ecomernce_api.security;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.dto.AuthResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.service.AuthService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Handles successful OAuth2 logins.
 *
 * <p>Mints JWT access + refresh tokens, attaches them as {@code HttpOnly} cookies,
 * then redirects to the configured frontend URL.
 *
 * <h3>Cross-origin cookie behaviour</h3>
 * When the Spring API and Next.js frontend live on <em>different</em> origins
 * (different hostnames or ports), the browser will only attach the cookies to
 * subsequent API requests when <strong>all three</strong> conditions hold:
 * <ol>
 *   <li>{@code SameSite=None}</li>
 *   <li>{@code Secure=true} (HTTPS required by the browser spec for SameSite=None)</li>
 *   <li>The frontend fetch/axios call uses {@code credentials: 'include'}</li>
 * </ol>
 * During local development over HTTP you can set {@code app.cookie.secure=false}
 * and accept {@code SameSite=Lax} — the cookies will then be set but will only
 * travel with same-site navigations. Use a reverse proxy (e.g. nginx or Next.js
 * rewrites) pointing both origins to the same hostname to avoid this entirely.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.success-redirect:http://localhost:3000}")
    private String frontendUrl;

    /** Set to {@code false} in local HTTP development via application properties. */
    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.jwt.access-token-expiry-minutes:15}")
    private int accessTokenExpiryMinutes;

    @Value("${app.jwt.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    private final AuthService authService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String registrationId = "unknown";
            if (authentication instanceof OAuth2AuthenticationToken token) {
                registrationId = token.getAuthorizedClientRegistrationId();
            }

            log.info("OAuth2 login via provider='{}' attributes={}", registrationId, oAuth2User.getAttributes());

            String email  = getEmail(registrationId, oAuth2User);
            String name   = getName(registrationId, oAuth2User);
            String avatar = getAvatar(registrationId, oAuth2User);

            if (email == null) {
                throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider");
            }

            // ── Upsert user ────────────────────────────────────────────────────────
            String finalRegistrationId = registrationId;
            User user = userRepository.findByEmail(email)
                    .map(existing -> {
                        updateExistingUser(existing, name, avatar);
                        return userRepository.save(existing);
                    })
                    .orElseGet(() -> {
                        User created = createNewUser(finalRegistrationId, email, name, avatar);
                        User saved = userRepository.save(created);
                        log.info("Created new OAuth2 user: email='{}' provider='{}'", email, finalRegistrationId);
                        return saved;
                    });

            log.info("User upsert complete, calling authService.getCurrentUserSession");

            // ── Get existing user session instead of creating new tokens ───────
            AuthResponse authResponse = authService.oauth2Login(user, request);

            log.info("Auth tokens generated, setting cookies");

            // ── Set cookies ────────────────────────────────────────────────────────
            // SameSite=None is required for cross-origin cookie delivery.
            // The spec mandates Secure=true whenever SameSite=None is used.
            String sameSite = secureCookie ? "None" : "Lax";

            ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", authResponse.getAccessToken())
                    .httpOnly(true)
                    .secure(secureCookie)
                    .path("/")
                    .maxAge(Duration.ofMinutes(accessTokenExpiryMinutes))
                    .sameSite(sameSite)
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", authResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(secureCookie)
                    .path("/")
                    .maxAge(Duration.ofDays(refreshTokenExpiryDays))
                    .sameSite(sameSite)
                    .build();

            response.addHeader("Set-Cookie", accessTokenCookie.toString());
            response.addHeader("Set-Cookie", refreshTokenCookie.toString());

            // Redirect to home page after successful OAuth2 login
            response.sendRedirect(frontendUrl);
            
        } catch (Exception e) {
            log.error("OAuth2 authentication success handler failed", e);
            response.sendRedirect(frontendUrl + "/auth/login?error=oauth2_failure");
        }
    }

    // ── Provider-specific attribute extraction ─────────────────────────────────

    private String getEmail(String provider, OAuth2User oAuth2User) {
        return switch (provider) {
            // GitHub may not expose the primary email in the basic profile scope.
            // Falling back to login@github.com is a reasonable substitute, but
            // ideally request the `user:email` scope and call /user/emails.
            case "github" -> oAuth2User.getAttribute("email") != null
                    ? oAuth2User.getAttribute("email")
                    : oAuth2User.getAttribute("login") + "@github.com";
            default -> oAuth2User.getAttribute("email");
        };
    }

    private String getName(String provider, OAuth2User oAuth2User) {
        return switch (provider) {
            case "github" -> oAuth2User.getAttribute("name") != null
                    ? oAuth2User.getAttribute("name")
                    : oAuth2User.getAttribute("login");
            default -> oAuth2User.getAttribute("name");
        };
    }

    private String getAvatar(String provider, OAuth2User oAuth2User) {
        return switch (provider) {
            case "github" -> oAuth2User.getAttribute("avatar_url");
            case "facebook" -> {
                Map<String, Object> picture = oAuth2User.getAttribute("picture");
                if (picture != null) {
                    Object dataObj = picture.get("data");
                    if (dataObj instanceof Map<?, ?> dataMap) {
                        Object urlObj = dataMap.get("url");
                        yield urlObj != null ? urlObj.toString() : null;
                    }
                }
                yield null;
            }
            default -> oAuth2User.getAttribute("picture");
        };
    }

    // ── User persistence helpers ───────────────────────────────────────────────

    private User createNewUser(String provider, String email, String name, String avatar) {
        String[] nameParts = parseName(name);
        return User.builder()
                .email(email)
                .username(generateUniqueUsername(email, provider))
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .profileImageUrl(avatar)
                .role(Role.USER)
                .isActive(true)
                .isLocked(false)
                .lastPasswordChange(LocalDateTime.now())
                .build();
    }

    private void updateExistingUser(User user, String name, String avatar) {
        if (name != null) {
            String[] nameParts = parseName(name);
            if (user.getFirstName() == null) user.setFirstName(nameParts[0]);
            if (user.getLastName() == null)  user.setLastName(nameParts[1]);
        }
        if (avatar != null && user.getProfileImageUrl() == null) {
            user.setProfileImageUrl(avatar);
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private String[] parseName(String name) {
        if (name != null && name.contains(" ")) {
            return name.split(" ", 2);
        }
        return new String[]{name != null ? name : "User", ""};
    }

    /**
     * Generates a username that is unique even when two users share the same
     * email prefix and provider, by appending a short random suffix when the
     * naive candidate is already taken.
     */
    private String generateUniqueUsername(String email, String provider) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        String providerSuffix = provider.substring(0, Math.min(3, provider.length()));
        String candidate = base + "_" + providerSuffix;

        // Retry with a random suffix until we find a free slot.
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + providerSuffix + "_" + UUID.randomUUID().toString().substring(0, 5);
        }
        return candidate;
    }
}