package com.smart_ecomernce_api.smart_ecomernce_api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CacheStatisticsService {
    private final CacheManager cacheManager;
    public CacheStatisticsService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    public Map<String, CacheStats> getAllCacheStatistics() {
        Map<String, CacheStats> stats = new HashMap<>();
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                stats.put(cacheName, getCacheStats(cache));
            }
        });
        return stats;
    }
    public CacheStats getCacheStats(Cache cache) {
        if (cache instanceof CaffeineCache caffeineCache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            com.github.benmanes.caffeine.cache.stats.CacheStats stats = nativeCache.stats();
            return new CacheStats(
                    cache.getName(),
                    stats.hitCount(),
                    stats.missCount(),
                    stats.hitRate(),
                    nativeCache.estimatedSize(),
                    stats.evictionCount()
            );
        }
        return new CacheStats(cache.getName(), 0, 0, 0, 0, 0);
    }
    public void clearAllCaches() {
        log.info("Clearing all caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }
    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public record CacheStats(
            String name,
            long hitCount,
            long missCount,
            double hitRate,
            long size,
            long evictionCount
    ) {}
}
