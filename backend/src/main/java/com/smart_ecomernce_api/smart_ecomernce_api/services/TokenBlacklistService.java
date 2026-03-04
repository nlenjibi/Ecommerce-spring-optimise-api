package com.smart_ecomernce_api.smart_ecomernce_api.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TokenBlacklistService {

    private final Cache<String, Boolean> tokenBlacklist;
    private final Cache<String, Long> tokenExpiryCache;
    private final Cache<String, Long> userTokenVersion;

    public TokenBlacklistService(
            @Value("${jwt.blacklist.max-size:10000}") int maxSize,
            @Value("${jwt.blacklist.expire-after-write-hours:24}") int expireAfterWriteHours) {
        
        this.tokenBlacklist = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteHours, TimeUnit.HOURS)
                .recordStats()
                .build();
        
        this.tokenExpiryCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteHours, TimeUnit.HOURS)
                .recordStats()
                .build();

        this.userTokenVersion = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(expireAfterWriteHours, TimeUnit.HOURS)
                .build();
        
        log.info("Token blacklist initialized with maxSize={}, expireAfterWriteHours={}", 
                maxSize, expireAfterWriteHours);
    }

    public void blacklistToken(String token, long expirationTime) {
        String tokenKey = hashToken(token);
        tokenBlacklist.put(tokenKey, true);
        
        long remainingTime = Math.max(expirationTime - System.currentTimeMillis(), 0);
        tokenExpiryCache.put(tokenKey, remainingTime);
        
        log.debug("Token blacklisted. Key: {}, remaining time: {}ms", tokenKey, remainingTime);
    }

    public boolean isTokenBlacklisted(String token) {
        String tokenKey = hashToken(token);
        Boolean isBlacklisted = tokenBlacklist.getIfPresent(tokenKey);
        
        if (isBlacklisted != null && isBlacklisted) {
            log.debug("Blacklisted token detected: {}", tokenKey.substring(0, 8) + "...");
            return true;
        }
        return false;
    }

    public void invalidateUserTokens(Long userId) {
        String userKey = "user_" + userId;
        long newVersion = System.currentTimeMillis();
        userTokenVersion.put(userKey, newVersion);
        log.info("Invalidated all tokens for user: {}", userId);
    }

    public boolean isUserTokenVersionValid(Long userId, Long tokenVersion) {
        String userKey = "user_" + userId;
        Long currentVersion = userTokenVersion.getIfPresent(userKey);
        if (currentVersion == null) {
            return true;
        }
        return tokenVersion == null || tokenVersion >= currentVersion;
    }

    public Long getUserTokenVersion(Long userId) {
        String userKey = "user_" + userId;
        return userTokenVersion.getIfPresent(userKey);
    }

    public void clearExpiredTokens() {
        tokenBlacklist.cleanUp();
        tokenExpiryCache.cleanUp();
        log.debug("Expired tokens cleared from blacklist");
    }

    public TokenBlacklistStats getStats() {
        return new TokenBlacklistStats(
                tokenBlacklist.estimatedSize(),
                tokenBlacklist.stats().hitRate(),
                tokenBlacklist.stats().missRate(),
                tokenExpiryCache.estimatedSize()
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available, falling back to hashCode", e);
            return String.valueOf(token.hashCode());
        }
    }

    public record TokenBlacklistStats(
            long currentSize,
            double hitRate,
            double missRate,
            long expiryCacheSize
    ) {}
}
