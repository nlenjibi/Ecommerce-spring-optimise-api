package com.smart_ecomernce_api.smart_ecomernce_api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Handles OAuth2 authentication failures.
 *
 * <p>On failure, sets a short-lived, JS-readable cookie containing the error
 * message and redirects the user to the frontend login page with an
 * {@code error} query parameter so the UI can display a contextual message
 * without having to parse the cookie itself.
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    /**
     * Frontend URL to redirect to on failure, e.g. {@code https://app.example.com/auth/login}.
     */
    @Value("${app.frontend.failure-redirect:http://localhost:3000/auth/login}")
    private String frontendFailureUrl;

    /**
     * Whether to set the {@code Secure} flag on cookies.
     * Set to {@code false} in local HTTP development.
     */
    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String message = resolveMessage(exception);

        if (exception instanceof AuthenticationServiceException) {
            log.error("OAuth2 authentication service error on '{}': {}",
                    request.getRequestURI(), exception.getMessage(), exception);
        } else {
            log.warn("OAuth2 authentication failure [{}] on '{}': {}",
                    exception.getClass().getSimpleName(), request.getRequestURI(), exception.getMessage());
        }

        // ── Error cookie ────────────────────────────────────────────────────────
        // httpOnly(false)  → JavaScript on the frontend CAN read this to display the error.
        // maxAge(60s)      → Very short-lived; this is a transient error signal, not persistent data.
        // SameSite=None    → Required for cross-origin requests (Next.js on a different domain).
        // Secure=true      → Required alongside SameSite=None per the spec; disable only in dev.
        ResponseCookie errorCookie = ResponseCookie.from("oauth2_error", encode(message))
                .httpOnly(false)             // Frontend JS must be able to read this
                .secure(secureCookie)
                .path("/")
                .maxAge(Duration.ofSeconds(60))  // Transient — expire quickly
                .sameSite(secureCookie ? "None" : "Lax")
                .build();

        response.addHeader("Set-Cookie", errorCookie.toString());

        // Also pass the error reason as a query parameter so the frontend can
        // display the message without relying solely on the cookie.
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendFailureUrl)
                .queryParam("error", encode(message))
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String resolveMessage(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oAuth2Ex) {
            String code = (oAuth2Ex.getError() != null)
                    ? oAuth2Ex.getError().getErrorCode()
                    : "unknown_error";
            return "OAuth2 provider returned an error: " + code + ". Please try again.";
        }
        if (ex instanceof BadCredentialsException) {
            return "The OAuth2 credentials are invalid.";
        }
        if (ex instanceof AccountStatusException) {
            return "Your account is not in a valid state. Please contact support.";
        }
        if (ex instanceof AuthenticationServiceException) {
            return "An internal error prevented OAuth2 login. Please try again later.";
        }
        return "OAuth2 login failed. Please try again or use a different login method.";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}