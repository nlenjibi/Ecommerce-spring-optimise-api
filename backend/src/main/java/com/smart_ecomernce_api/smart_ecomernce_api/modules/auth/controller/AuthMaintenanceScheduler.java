package com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.service.AuthService;
import com.smart_ecomernce_api.smart_ecomernce_api.services.SecurityEventService;
import com.smart_ecomernce_api.smart_ecomernce_api.services.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled maintenance tasks for auth and security subsystems.
 *
 * NOTE: This class lives in the `scheduler` package, not `controller`.
 * Schedulers are infrastructure components, not request handlers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthMaintenanceScheduler {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityEventService securityEventService;

    /**
     * Runs nightly at 02:00 server time.
     *
     * Execution order matters:
     *   1. Invalidate expired DB sessions first (source of truth).
     *   2. Clear the in-memory token blacklist (derived cache).
     *   3. Clear stale security event records (audit / rate-limit data).
     *
     * Each step is wrapped independently so a failure in one does not
     * prevent the others from running.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runNightlyMaintenance() {
        log.info("=== Auth maintenance job started ===");

        runSafely("session cleanup",  authService::cleanupExpiredSessions);
        runSafely("blacklist cleanup", tokenBlacklistService::clearExpiredTokens);
        runSafely("security events cleanup", securityEventService::clearExpiredAttempts);

        log.info("=== Auth maintenance job completed ===");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Runs a maintenance step and logs any exception without propagating it,
     * so that one failed step never aborts the rest of the job.
     */
    private void runSafely(String stepName, Runnable step) {
        try {
            log.debug("Running maintenance step: {}", stepName);
            step.run();
            log.debug("Completed maintenance step: {}", stepName);
        } catch (Exception e) {
            // Log at ERROR so alerting can pick it up, but do not re-throw.
            log.error("Maintenance step '{}' failed: {}", stepName, e.getMessage(), e);
        }
    }
}