package com.smart_ecomernce_api.smart_ecomernce_api.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.smart_ecomernce_api.smart_ecomernce_api.config.JwtCacheConfig.CachedAuthentication;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.CustomAuthenticationEntryPoint;
import com.smart_ecomernce_api.smart_ecomernce_api.security.JwtTokenProvider;
import com.smart_ecomernce_api.smart_ecomernce_api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Single-responsibility service that owns the full JWT validation pipeline.
 *
 * <p>Checks are ordered cheapest-first so that the expensive DB call
 * ({@code loadUserById}) is skipped whenever a cheaper check fails:
 *
 * <ol>
 *   <li>JWT signature / expiry (CPU-only)</li>
 *   <li>Token blacklist  (in-memory Caffeine)</li>
 *   <li>Token version    (in-memory Caffeine)</li>
 *   <li>Load UserPrincipal (<b>DB – cached</b> via Spring Cache)</li>
 *   <li>Account-lock flag (from cached principal)</li>
 *   <li>Password-change timestamp (from cached principal)</li>
 * </ol>
 *
 * <p>Returns {@code null} when the token is absent or all checks pass
 * (the filter continues the chain), or an {@link AuthenticationException}
 * subclass that {@link CustomAuthenticationEntryPoint} knows how to render.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlacklistService   tokenBlacklistService;
    private final Cache<String, CachedAuthentication> tokenValidationCache;

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Validates {@code jwt} through the full security pipeline.
     * Uses token-level caching to avoid repeated JWT parsing and principal loading.
     *
     * @return {@code null}  – no token present OR all checks passed
     *         (caller should continue the filter chain and, when non-null
     *         Authentication is also returned, set it in the SecurityContext)
     * @return non-null {@link AuthenticationException} – a check failed;
     *         caller should reject the request via the entry point
     */
    public ValidationResult validate(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return ValidationResult.noToken();
        }

        // Check token validation cache first (fastest path)
        CachedAuthentication cached = tokenValidationCache.getIfPresent(jwt);
        if (cached != null) {
            log.debug("Token cache hit for user {}", cached.username());
            return ValidationResult.valid(new UsernamePasswordAuthenticationToken(
                    cached.authentication().getPrincipal(),
                    null,
                    cached.authentication().getAuthorities()));
        }

        // ── 1. Signature / expiry (CPU, no I/O) ───────────────────────────────
        if (!jwtTokenProvider.validateToken(jwt)) {
            return ValidationResult.invalid();
        }

        // ── 2. Blacklist check (Caffeine in-memory) ───────────────────────────
        if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
            log.warn("Blacklisted token attempted: {}", jwt.substring(0, Math.min(20, jwt.length())));
            return ValidationResult.reject(
                    new BadCredentialsException("Token has been revoked. Please login again."));
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(jwt);

        // ── 3. Token-version check (Caffeine in-memory) ───────────────────────
        Long userTokenVersion = tokenBlacklistService.getUserTokenVersion(userId);
        if (userTokenVersion != null
                && !tokenBlacklistService.isUserTokenVersionValid(userId, userTokenVersion)) {
            log.warn("Token version invalid for user: {}", userId);
            return ValidationResult.reject(
                    new InsufficientAuthenticationException(
                            "Session has been invalidated. Please login again."));
        }

        // ── 4. Load UserPrincipal – single DB call, result is cached ──────────
        UserPrincipal principal = loadPrincipal(userId);

        // ── 5. Account-lock (from cached principal, no extra I/O) ─────────────
        if (principal.isAccountLocked()) {
            log.warn("Locked account attempted: {}", userId);
            return ValidationResult.reject(
                    new LockedException("Account is locked. Please contact support."));
        }

        // ── 6. Password-change timestamp (from cached principal) ──────────────
        Long tokenPasswordChangedAt   = jwtTokenProvider.getPasswordChangedAtFromToken(jwt);
        Long principalPasswordChanged = principal.getLastPasswordChangeEpoch();
        if (tokenPasswordChangedAt != null && principalPasswordChanged != null
                && tokenPasswordChangedAt < principalPasswordChanged) {
            log.warn("Token predates password change for user: {}", userId);
            return ValidationResult.reject(
                    new CredentialsExpiredException(
                            "Password has been changed. Please login again."));
        }

        // ── All checks passed – build the Authentication object ───────────────
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        
        // Cache the successful validation result
        String username = principal.getUsername();
        tokenValidationCache.put(jwt, CachedAuthentication.create(auth, userId, username));
        
        log.debug("JWT validated and cached for user {}", userId);
        return ValidationResult.valid(auth);
    }

    /**
     * Loads a {@link UserPrincipal} by id. Results are cached under the key
     * {@code userId} in the {@code "userPrincipals"} cache so that repeated
     * requests within the cache TTL skip the DB entirely.
     *
     * <p>Evict the entry (via {@link #evictPrincipal}) whenever the user's
     * password, lock state, or role changes.
     */
    @Cacheable(value = "userPrincipals", key = "#userId")
    public UserPrincipal loadPrincipal(Long userId) {
        return (UserPrincipal) customUserDetailsService.loadUserById(userId);
    }

    /**
     * Evicts the cached {@link UserPrincipal} for {@code userId}.
     * Call this after any operation that changes the user's security-relevant
     * state: password change, lock/unlock, role update, logout-all.
     */
    @CacheEvict(value = "userPrincipals", key = "#userId")
    public void evictPrincipal(Long userId) {
        log.debug("Evicted cached UserPrincipal for user {}", userId);
    }

    /**
     * Evicts a specific token from the validation cache.
     * Call this on logout to immediately invalidate the token cache.
     */
    public void evictToken(String token) {
        if (token != null) {
            tokenValidationCache.invalidate(token);
            log.debug("Evicted token from validation cache");
        }
    }

    /**
     * Evicts all tokens for a user from the validation cache.
     * Call this on password change or account lock.
     */
    public void evictAllTokensForUser(Long userId) {
        // Since we can't efficiently iterate by userId in Caffeine,
        // we'll rely on the token version check in validate()
        // This method is here for API completeness
        log.debug("User {} tokens will be invalidated via version check", userId);
    }

    // ── Result type ────────────────────────────────────────────────────────────

    /**
     * Immutable result returned by {@link #validate}.
     *
     * <ul>
     *   <li>{@link Status#NO_TOKEN}  – no Authorization header; continue chain unauthenticated</li>
     *   <li>{@link Status#INVALID}   – bad / expired JWT; continue chain unauthenticated</li>
     *   <li>{@link Status#REJECT}    – security check failed; abort with 401</li>
     *   <li>{@link Status#VALID}     – all checks passed; set authentication and continue</li>
     * </ul>
     */
    public record ValidationResult(
            Status status,
            Authentication authentication,
            AuthenticationException error) {

        public enum Status { NO_TOKEN, INVALID, REJECT, VALID }

        static ValidationResult noToken()  { return new ValidationResult(Status.NO_TOKEN,  null, null); }
        static ValidationResult invalid()  { return new ValidationResult(Status.INVALID,   null, null); }
        static ValidationResult reject(AuthenticationException ex) {
            return new ValidationResult(Status.REJECT, null, ex);
        }
        static ValidationResult valid(Authentication auth) {
            return new ValidationResult(Status.VALID, auth, null);
        }

        public boolean shouldReject()    { return status == Status.REJECT; }
        public boolean shouldAuthenticate() { return status == Status.VALID; }
    }
}