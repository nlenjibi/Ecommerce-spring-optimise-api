package com.smart_ecomernce_api.smart_ecomernce_api.security;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.CustomAuthenticationEntryPoint;
import com.smart_ecomernce_api.smart_ecomernce_api.services.TokenValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Minimal JWT authentication filter with request-scoped caching.
 *
 * <p>
 * All validation logic lives in {@link TokenValidationService};
 * this class has a single responsibility: extract the token from the
 * request, hand it to the service, then either set the SecurityContext
 * or reject the request via {@link CustomAuthenticationEntryPoint}.
 * 
 * <p>
 * Optimizations:
 * - Token validation results are cached via TokenValidationService (Caffeine)
 * - Request-scoped ThreadLocal prevents re-extraction within same request
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenValidationService tokenValidationService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    private static final ThreadLocal<String> currentToken = new ThreadLocal<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getOrExtractJwt(request);
            TokenValidationService.ValidationResult result = tokenValidationService.validate(jwt);

            if (result.shouldReject()) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response, result.error());
                return;
            }

            if (result.shouldAuthenticate()) {
                UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) result
                        .authentication();
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (Exception ex) {
            log.error("Unexpected error in JWT filter", ex);
        } finally {
            currentToken.remove();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Gets cached token from ThreadLocal or extracts from request.
     * This prevents multiple token extractions within the same request.
     */
    private String getOrExtractJwt(HttpServletRequest request) {
        String cached = currentToken.get();
        if (cached != null) {
            return cached;
        }

        String jwt = extractJwt(request);
        if (jwt != null) {
            currentToken.set(jwt);
        }
        return jwt;
    }

    private String extractJwt(HttpServletRequest request) {
        // First, try to extract from Authorization header
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        // For OAuth2 with HttpOnly cookies, also try to extract from cookies
        // Check for access_token cookie (set by OAuth2AuthenticationSuccessHandler)
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("Extracted JWT from access_token cookie");
                        return token;
                    }
                }
                // Also check for auth-token cookie (alternative name)
                if ("auth-token".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("Extracted JWT from auth-token cookie");
                        return token;
                    }
                }
            }
        }

        return null;
    }
}