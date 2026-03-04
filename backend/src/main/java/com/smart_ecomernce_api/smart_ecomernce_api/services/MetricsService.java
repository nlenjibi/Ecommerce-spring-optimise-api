package com.smart_ecomernce_api.smart_ecomernce_api.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.smart_ecomernce_api.smart_ecomernce_api.aspect.RateLimitingAspect;
import com.smart_ecomernce_api.smart_ecomernce_api.config.JwtCacheConfig.CachedAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics Service
 * 
 * Provides runtime performance metrics collection and reporting.
 * Monitors:
 * - Cache hit rates
 * - Memory usage
 * - Thread pool statistics
 * - Request rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final RateLimitingAspect rateLimitingAspect;
    private final Cache<String, CachedAuthentication> tokenValidationCache;

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    private final Map<String, AtomicLong> customMetrics = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 60000) // Every minute
    public void collectMetrics() {
        log.debug("Collecting performance metrics...");
        
        // Log memory usage
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        log.info("Heap Memory: used={}MB, max={}MB, usage={}%",
            heapUsage.getUsed() / 1024 / 1024,
            heapUsage.getMax() / 1024 / 1024,
            (int) (heapUsage.getUsed() * 100.0 / heapUsage.getMax()));
        
        // Log thread count
        log.info("Threads: count={}, peak={}", 
            threadBean.getThreadCount(), 
            threadBean.getPeakThreadCount());
        
        // Log cache statistics
        if (tokenValidationCache != null) {
            CacheStats stats = tokenValidationCache.stats();
            log.info("Token Cache: hitRate={}%, hits={}, misses={}, size={}",
                String.format("%.2f", stats.hitRate() * 100),
                stats.hitCount(),
                stats.missCount(),
                tokenValidationCache.estimatedSize());
        }
    }

    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Memory metrics
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        
        metrics.put("heapUsed", heap.getUsed());
        metrics.put("heapMax", heap.getMax());
        metrics.put("heapUsedPercent", (int) (heap.getUsed() * 100.0 / heap.getMax()));
        metrics.put("nonHeapUsed", nonHeap.getUsed());
        
        // Thread metrics
        metrics.put("threadCount", threadBean.getThreadCount());
        metrics.put("peakThreadCount", threadBean.getPeakThreadCount());
        metrics.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        
        return metrics;
    }

    public Map<String, Object> getCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        if (tokenValidationCache != null) {
            CacheStats stats = tokenValidationCache.stats();
            metrics.put("tokenCacheHitRate", stats.hitRate());
            metrics.put("tokenCacheHits", stats.hitCount());
            metrics.put("tokenCacheMisses", stats.missCount());
            metrics.put("tokenCacheSize", tokenValidationCache.estimatedSize());
            metrics.put("tokenCacheEvictionCount", stats.evictionCount());
        }
        
        return metrics;
    }

    public Map<String, Object> getRateLimitMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        if (rateLimitingAspect != null) {
            var stats = rateLimitingAspect.getStatistics();
            metrics.put("endpointCount", stats.size());
            metrics.put("endpoints", stats);
        }
        
        return metrics;
    }

    public Map<String, Object> getAllMetrics() {
        Map<String, Object> allMetrics = new HashMap<>();
        allMetrics.put("system", getSystemMetrics());
        allMetrics.put("cache", getCacheMetrics());
        allMetrics.put("rateLimit", getRateLimitMetrics());
        
        // Custom metrics
        Map<String, Long> custom = new HashMap<>();
        customMetrics.forEach((key, value) -> custom.put(key, value.get()));
        allMetrics.put("custom", custom);
        
        return allMetrics;
    }

}
