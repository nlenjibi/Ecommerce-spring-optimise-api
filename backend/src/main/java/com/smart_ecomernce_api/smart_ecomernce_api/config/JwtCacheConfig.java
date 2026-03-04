package com.smart_ecomernce_api.smart_ecomernce_api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class JwtCacheConfig {

    @Value("${jwt.cache.max-size:10000}")
    private int maxSize;

    @Value("${jwt.cache.expire-after-write-minutes:10}")
    private int expireAfterWriteMinutes;

    @Value("${jwt.cache.expire-after-access-minutes:5}")
    private int expireAfterAccessMinutes;

    @Bean
    public Cache<String, CachedAuthentication> tokenValidationCache() {
        Cache<String, CachedAuthentication> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .expireAfterAccess(expireAfterAccessMinutes, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((Object key, CachedAuthentication value, com.github.benmanes.caffeine.cache.RemovalCause cause) -> {
                    if (value != null && key instanceof String) {
                        String keyStr = (String) key;
                        log.debug("Token cache entry removed: key={}, cause={}", 
                            keyStr.substring(0, Math.min(10, keyStr.length())), cause);
                    }
                })
                .build();

        log.info("JWT token validation cache initialized: maxSize={}, expireAfterWrite={}min, expireAfterAccess={}min",
                maxSize, expireAfterWriteMinutes, expireAfterAccessMinutes);
        
        return cache;
    }

    public record CachedAuthentication(
            Authentication authentication,
            Long userId,
            String username,
            long cachedAt
    ) {
        public static CachedAuthentication create(Authentication auth, Long userId, String username) {
            return new CachedAuthentication(auth, userId, username, System.currentTimeMillis());
        }
    }
}
