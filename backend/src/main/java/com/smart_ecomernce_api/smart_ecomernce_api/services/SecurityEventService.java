package com.smart_ecomernce_api.smart_ecomernce_api.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

@Service
@Slf4j
public class SecurityEventService {

    private final Cache<String, Integer> failedLoginAttempts;
    private final Map<String, AccessLogEntry> accessLog;
    private final StampedLock accessLogLock = new StampedLock();
    private final int maxFailedAttempts;
    private final int lockoutDurationMinutes;

    private static final int MAX_ACCESS_LOG_ENTRIES = 1000;

    public SecurityEventService(
            @Value("${security.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${security.lockout-duration-minutes:30}") int lockoutDurationMinutes) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
        
        this.failedLoginAttempts = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(lockoutDurationMinutes * 2, TimeUnit.MINUTES)
                .recordStats()
                .build();
        
        this.accessLog = new ConcurrentHashMap<>();
        
        log.info("SecurityEventService initialized with maxFailedAttempts={}, lockoutDurationMinutes={}", 
                maxFailedAttempts, lockoutDurationMinutes);
    }

    public void recordLoginSuccess(String identifier, String ipAddress) {
        failedLoginAttempts.invalidate(identifier.toLowerCase());
        
        log.info("LOGIN_SUCCESS: user={}, ip={}, time={}", identifier, ipAddress, LocalDateTime.now());
        
        addAccessLogEntry(new AccessLogEntry(
                identifier,
                "LOGIN_SUCCESS",
                ipAddress,
                LocalDateTime.now(),
                "Successful login"
        ));
    }

    public void recordLoginFailure(String identifier, String ipAddress, String reason) {
        String key = identifier.toLowerCase();
        int attempts = failedLoginAttempts.get(key, k -> 0) + 1;
        failedLoginAttempts.put(key, attempts);
        
        log.warn("LOGIN_FAILURE: user={}, ip={}, attempts={}, reason={}, time={}", 
                identifier, ipAddress, attempts, reason, LocalDateTime.now());
        
        boolean accountLocked = attempts >= maxFailedAttempts;
        
        addAccessLogEntry(new AccessLogEntry(
                identifier,
                "LOGIN_FAILURE",
                ipAddress,
                LocalDateTime.now(),
                reason + (accountLocked ? " [ACCOUNT LOCKED]" : "")
        ));
        
        if (accountLocked) {
            log.error("ACCOUNT_LOCKED: user={}, ip={}, failedAttempts={}", 
                    identifier, ipAddress, attempts);
        }
    }

    public void recordTokenRevoked(String identifier, String reason) {
        log.info("TOKEN_REVOKED: user={}, reason={}, time={}", 
                identifier, reason, LocalDateTime.now());
    }

    public void recordOAuth2Login(String identifier, String provider, String ipAddress) {
        log.info("OAUTH2_LOGIN: user={}, provider={}, ip={}, time={}", 
                identifier, provider, ipAddress, LocalDateTime.now());
    }

    public void recordAccountLocked(String identifier, String lockedBy, String reason) {
        log.warn("ACCOUNT_LOCKED: user={}, lockedBy={}, reason={}, time={}", 
                identifier, lockedBy, reason, LocalDateTime.now());
        
        addAccessLogEntry(new AccessLogEntry(
                identifier,
                "ACCOUNT_LOCKED",
                lockedBy,
                LocalDateTime.now(),
                reason
        ));
    }

    public void recordAccountUnlocked(String identifier, String unlockedBy) {
        log.info("ACCOUNT_UNLOCKED: user={}, unlockedBy={}, time={}", 
                identifier, unlockedBy, LocalDateTime.now());
        
        addAccessLogEntry(new AccessLogEntry(
                identifier,
                "ACCOUNT_UNLOCKED",
                unlockedBy,
                LocalDateTime.now(),
                "Account unlocked by " + unlockedBy
        ));
    }

    /**
     * Add entry to access log with StampedLock for thread safety
     */
    private void addAccessLogEntry(AccessLogEntry entry) {
        long stamp = accessLogLock.writeLock();
        try {
            String key = entry.identifier().toLowerCase();
            accessLog.put(key, entry);
            // Keep only last MAX_ACCESS_LOG_ENTRIES
            if (accessLog.size() > MAX_ACCESS_LOG_ENTRIES) {
                // Remove oldest entries (simple approach - just clear half)
                List<String> keysToRemove = new ArrayList<>(accessLog.keySet());
                for (int i = 0; i < keysToRemove.size() / 2; i++) {
                    accessLog.remove(keysToRemove.get(i));
                }
            }
        } finally {
            accessLogLock.unlockWrite(stamp);
        }
    }

    public int getFailedLoginAttempts(String identifier) {
        Integer attempts = failedLoginAttempts.getIfPresent(identifier.toLowerCase());
        return attempts != null ? attempts : 0;
    }

    public void clearExpiredAttempts() {
        failedLoginAttempts.cleanUp();
    }

    public SecurityStats getStats() {
        return new SecurityStats(
                failedLoginAttempts.estimatedSize(),
                failedLoginAttempts.stats().hitRate(),
                getAccessLogSize(),
                maxFailedAttempts,
                lockoutDurationMinutes
        );
    }

    /**
     * Get access log size with optimistic read
     */
    private int getAccessLogSize() {
        long stamp = accessLogLock.tryOptimisticRead();
        int size = accessLog.size();
        if (!accessLogLock.validate(stamp)) {
            stamp = accessLogLock.readLock();
            try {
                size = accessLog.size();
            } finally {
                accessLogLock.unlockRead(stamp);
            }
        }
        return size;
    }

    public record AccessLogEntry(
            String identifier,
            String eventType,
            String ipAddress,
            LocalDateTime timestamp,
            String details
    ) {}

    public record SecurityStats(
            long currentFailedAttemptsCount,
            double hitRate,
            int accessLogSize,
            int maxFailedAttempts,
            int lockoutDurationMinutes
    ) {}
}
