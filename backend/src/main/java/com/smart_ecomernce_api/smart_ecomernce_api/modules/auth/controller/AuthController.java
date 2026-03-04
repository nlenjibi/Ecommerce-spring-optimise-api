package com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.service.AuthService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.ChangePasswordRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/auth")
@Tag(name = "Authentication", description = "User authentication and session management")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        log.info("Registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    // create a /me endpoint to get the current user
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get the current authenticated user with tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        // Validate that user is authenticated
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not authenticated"));
        }

        // Validate that user ID is present
        if (principal.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid user token"));
        }

        try {
            // Get the current user session with tokens using user ID from principal
            AuthResponse authResponse = authService.getCurrentUserSessionById(principal.getId());

            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", authResponse));
        } catch (com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException e) {
            // User was deleted but token is still valid - treat as unauthenticated
            log.warn("User {} not found - likely deleted, token may be stale", principal.getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User account no longer exists"));
        } catch (Exception e) {
            log.error("Error getting current user session: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user session"));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with credentials")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        // Log only a masked or partial identifier — never log the full email in prod if
        // PII-sensitive
        log.info("Login request for email: {}", maskEmail(request.getEmail()));
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Obtain a new access token using a refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        log.info("Token refresh request from IP: {}", getClientIp(httpRequest));
        AuthResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate current session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        log.info("Logout request received");
        authService.logout(request.getRefreshToken(), httpRequest, response);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    /**
     * Password change uses a request body (not @RequestParam) so credentials
     * are never exposed in server access logs or browser history.
     */
    @PostMapping("/password/change")
    @Operation(summary = "Change password", description = "Change password for the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Password change request for user ID: {}", currentUser.getId());
        authService.changePassword(currentUser.getId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /**
     * Admin-only endpoints are enforced at the method level via @PreAuthorize,
     * not just by convention or documentation.
     */
    @PostMapping("/account/lock")
    @Operation(summary = "Lock account", description = "Lock a user account (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockAccount(
            @Valid @RequestBody AccountActionRequest request) {
        log.info("Admin locking account for user ID: {} - Reason: {}", request.getUserId(), request.getReason());
        authService.lockAccount(request.getUserId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Account locked successfully", null));
    }

    @PostMapping("/account/unlock")
    @Operation(summary = "Unlock account", description = "Unlock a user account (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(
            @Valid @RequestBody AccountActionRequest request) {
        log.info("Admin unlocking account for user ID: {}", request.getUserId());
        authService.unlockAccount(request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Account unlocked successfully", null));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        return (local.length() > 2 ? local.charAt(0) + "***" + local.charAt(local.length() - 1) : "***")
                + "@" + parts[1];
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}