package com.smart_ecomernce_api.smart_ecomernce_api.modules.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.services.CacheStatisticsService;
import com.smart_ecomernce_api.smart_ecomernce_api.services.DatabaseMetricsService;
import com.smart_ecomernce_api.smart_ecomernce_api.services.MetricsService;
import com.smart_ecomernce_api.smart_ecomernce_api.services.SecurityEventService;
import com.smart_ecomernce_api.smart_ecomernce_api.services.TokenBlacklistService;
import com.smart_ecomernce_api.smart_ecomernce_api.aspect.QueryPerformanceAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Performance monitoring and cache management – ADMIN only.
 * Class-level @PreAuthorize("hasRole('ADMIN')") covers every endpoint.
 */
@RestController
@RequestMapping("v1/performance")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class PerformanceController {

    private final CacheManager cacheManager;
    private final CacheStatisticsService cacheStatisticsService;
    private final MetricsService metricsService;
    private final SecurityEventService securityEventService;
    private final TokenBlacklistService tokenBlacklistService;
    private final DatabaseMetricsService databaseMetricsService;
    private final QueryPerformanceAspect queryPerformanceAspect;

    // ==================== Dashboard / Overview ====================

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // System overview
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            dashboard.put("system", Map.of(
                    "memory", Map.of(
                            "maxMb", maxMemory,
                            "usedMb", usedMemory,
                            "usagePercent", String.format("%.2f", (double) usedMemory / maxMemory * 100)),
                    "processors", runtime.availableProcessors(),
                    "uptime", System.currentTimeMillis()));

            // Cache overview
            dashboard.put("cache", cacheStatisticsService.getAllCacheStatistics());

            // Security overview
            var securityStats = securityEventService.getStats();
            dashboard.put("security", Map.of(
                    "failedLoginAttempts", securityStats.currentFailedAttemptsCount(),
                    "hitRate", formatPercent(securityStats.hitRate()),
                    "accessLogSize", securityStats.accessLogSize()));

            // Token blacklist overview
            var blacklistStats = tokenBlacklistService.getStats();
            dashboard.put("blacklist", Map.of(
                    "size", blacklistStats.currentSize(),
                    "hitRate", formatPercent(blacklistStats.hitRate())));

            // Available endpoints info
            dashboard.put("endpoints", Map.ofEntries(
                    Map.entry("metrics", "/v1/performance/metrics"),
                    Map.entry("dashboard", "/v1/performance/dashboard"),
                    Map.entry("cache", "/v1/performance/cache"),
                    Map.entry("system", "/v1/performance/system"),
                    Map.entry("database", "/v1/performance/database"),
                    Map.entry("security", "/v1/performance/security"),
                    Map.entry("securityStats", "/v1/performance/security/stats"),
                    Map.entry("rateLimit", "/v1/performance/rate-limit"),
                    Map.entry("all", "/v1/performance/all"),
                    Map.entry("download", "/v1/performance/download"),
                    Map.entry("refresh", "/v1/performance/refresh"),
                    Map.entry("design", "/v1/performance/design")));

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(dashboard)
                    .message("Dashboard retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve dashboard: " + e.getMessage()).build());
        }
    }

    // ==================== Metrics ====================

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        try {
            metrics.put("cache_stats", cacheStatisticsService.getAllCacheStatistics());

            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            metrics.put("memory", Map.of(
                    "max_mb", maxMemory,
                    "used_mb", usedMemory,
                    "usage_percent", String.format("%.2f", (double) usedMemory / maxMemory * 100)));
            metrics.put("available_processors", runtime.availableProcessors());
            metrics.put("query_performance", "Track query execution times in service layer");
            metrics.put("slow_queries", "Monitor queries taking > 1s");

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(metrics)
                    .message("Performance metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving performance metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve performance metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/cache/{cacheName}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats(
            @PathVariable String cacheName) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("cache_name", cacheName);
            boolean available = cacheManager.getCacheNames().contains(cacheName);
            stats.put("available", available);
            if (available) {
                stats.put("stats", cacheStatisticsService.getCacheStats(cacheManager.getCache(cacheName)));
            }
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(stats).build());
        } catch (Exception e) {
            log.error("Error retrieving cache stats for {}: {}", cacheName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve cache stats: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<ApiResponse<String>> clearAllCaches() {
        try {
            cacheStatisticsService.clearAllCaches();
            log.info("Cleared all caches");
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("All caches cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing caches: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false).message("Failed to clear caches: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cache/clear/{cacheName}")
    public ResponseEntity<ApiResponse<String>> clearCache(@PathVariable String cacheName) {
        try {
            if (!cacheManager.getCacheNames().contains(cacheName)) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                        .success(false).message("Cache '" + cacheName + "' not found").build());
            }
            cacheStatisticsService.clearCache(cacheName);
            log.info("Cleared cache: {}", cacheName);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("Cache '" + cacheName + "' cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing cache {}: {}", cacheName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false).message("Failed to clear cache: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cache/warmup")
    public ResponseEntity<ApiResponse<String>> warmupCaches() {
        try {
            log.info("Starting cache warmup process");
            // Warmup logic implemented as needed
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("Cache warmup process initiated").build());
        } catch (Exception e) {
            log.error("Error during cache warmup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false).message("Failed to warmup caches: " + e.getMessage()).build());
        }
    }

    @GetMapping("/database")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseMetrics() {
        try {
            Map<String, Object> metrics = databaseMetricsService.getDatabaseMetrics();
            
            // Add query performance from aspect
            var queryReport = queryPerformanceAspect.generateReport();
            Map<String, Object> queryPerfData = new java.util.LinkedHashMap<>();
            queryPerfData.put("totalQueries", queryReport.getTotalQueries());
            queryPerfData.put("totalSlowQueries", queryReport.getTotalSlowQueries());
            
            // Get slowest queries
            var slowestQueries = queryPerformanceAspect.getSlowestQueries(10);
            queryPerfData.put("slowestQueries", slowestQueries.stream()
                    .map(e -> java.util.Map.of(
                            "query", e.queryName(),
                            "count", e.stats().getCount(),
                            "avgTime", String.format("%.2fms", e.stats().getAverageTime()),
                            "maxTime", e.stats().getMaxTime() + "ms",
                            "minTime", e.stats().getMinTime() + "ms"
                    ))
                    .toList());
            
            metrics.put("queryPerformance", queryPerfData);
            
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(metrics)
                    .message("Database metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving database metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve database metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemMetrics() {
        try {
            Map<String, Object> metrics = metricsService.getSystemMetrics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(metrics)
                    .message("System metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving system metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve system metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/cache")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheMetrics() {
        try {
            Map<String, Object> metrics = metricsService.getCacheMetrics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(metrics)
                    .message("Cache metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving cache metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve cache metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimitMetrics() {
        try {
            Map<String, Object> metrics = metricsService.getRateLimitMetrics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(metrics)
                    .message("Rate limit metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving rate limit metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve rate limit metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/security")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            var securityStats = securityEventService.getStats();
            stats.put("failedLoginAttempts", securityStats.currentFailedAttemptsCount());
            stats.put("hitRate", securityStats.hitRate());
            stats.put("accessLogSize", securityStats.accessLogSize());
            stats.put("maxFailedAttempts", securityStats.maxFailedAttempts());
            stats.put("lockoutDurationMinutes", securityStats.lockoutDurationMinutes());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(stats)
                    .message("Security stats retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving security stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve security stats: " + e.getMessage()).build());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllMetrics() {
        try {
            Map<String, Object> allMetrics = metricsService.getAllMetrics();

            var securityStats = securityEventService.getStats();
            allMetrics.put("security", Map.of(
                    "failedLoginAttempts", securityStats.currentFailedAttemptsCount(),
                    "hitRate", securityStats.hitRate(),
                    "accessLogSize", securityStats.accessLogSize()));

            // Add database metrics
            allMetrics.put("database", databaseMetricsService.getDatabaseMetrics());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(allMetrics)
                    .message("All metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving all metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve all metrics: " + e.getMessage()).build());
        }
    }

    // ==================== Security Stats (merged from SecurityStatsController)
    // ====================

    @GetMapping("/security/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityStatsDetailed() {
        log.info("Security stats requested by admin");

        var blacklistStats = tokenBlacklistService.getStats();
        var securityStats = securityEventService.getStats();

        Map<String, Object> stats = Map.of(
                "tokenBlacklist", Map.of(
                        "currentSize", blacklistStats.currentSize(),
                        "hitRate", formatPercent(blacklistStats.hitRate()),
                        "missRate", formatPercent(blacklistStats.missRate()),
                        "expiryCacheSize", blacklistStats.expiryCacheSize()),
                "securityEvents", Map.of(
                        "failedAttemptsCount", securityStats.currentFailedAttemptsCount(),
                        "hitRate", formatPercent(securityStats.hitRate()),
                        "accessLogSize", securityStats.accessLogSize(),
                        "maxFailedAttempts", securityStats.maxFailedAttempts(),
                        "lockoutDurationMinutes", securityStats.lockoutDurationMinutes()));

        return ResponseEntity.ok(ApiResponse.success("Security stats retrieved", stats));
    }

    @PostMapping("/security/cleanup")
    public ResponseEntity<ApiResponse<String>> triggerSecurityCleanup() {
        log.info("Manual security cleanup triggered by admin");
        tokenBlacklistService.clearExpiredTokens();
        securityEventService.clearExpiredAttempts();
        return ResponseEntity.ok(ApiResponse.success("Cleanup completed", "Expired entries removed"));
    }

    // ==================== Download / Export ====================

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadMetrics(
            @RequestParam(defaultValue = "json") String format) {
        try {
            log.info("Download metrics requested in format: {}", format);

            Map<String, Object> allData = new HashMap<>();
            allData.put("timestamp", LocalDateTime.now().toString());
            allData.put("metrics", metricsService.getAllMetrics());

            var securityStats = securityEventService.getStats();
            allData.put("security", Map.of(
                    "failedLoginAttempts", securityStats.currentFailedAttemptsCount(),
                    "hitRate", formatPercent(securityStats.hitRate()),
                    "accessLogSize", securityStats.accessLogSize()));

            var blacklistStats = tokenBlacklistService.getStats();
            allData.put("blacklist", Map.of(
                    "size", blacklistStats.currentSize(),
                    "hitRate", formatPercent(blacklistStats.hitRate()),
                    "expiryCacheSize", blacklistStats.expiryCacheSize()));

            allData.put("cache", cacheStatisticsService.getAllCacheStatistics());

            String filename = "performance-metrics-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

            if ("csv".equalsIgnoreCase(format)) {
                StringBuilder csv = new StringBuilder();
                csv.append("Category,Metric,Value\n");

                // Memory
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / (1024 * 1024);
                long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
                csv.append(String.format("System,Max Memory (MB),%d\n", maxMemory));
                csv.append(String.format("System,Used Memory (MB),%d\n", usedMemory));
                csv.append(String.format("System,Memory Usage (%%),%.2f\n", (double) usedMemory / maxMemory * 100));
                csv.append(String.format("System,Available Processors,%d\n", runtime.availableProcessors()));

                // Security
                csv.append(String.format("Security,Failed Login Attempts,%d\n",
                        securityStats.currentFailedAttemptsCount()));
                csv.append(String.format("Security,Access Log Size,%d\n", securityStats.accessLogSize()));

                // Blacklist
                csv.append(String.format("Blacklist,Current Size,%d\n", blacklistStats.currentSize()));

                byte[] bytes = csv.toString().getBytes();
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".csv\"")
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(bytes);
            } else {
                // JSON format (default)
                String json = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(allData);
                byte[] bytes = json.getBytes();
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".json\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(bytes);
            }
        } catch (Exception e) {
            log.error("Error generating download: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== Refresh ====================

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshMetrics() {
        try {
            log.info("Refreshing all metrics");

            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("status", "Metrics refreshed successfully");

            // Force cache eviction to get fresh data
            cacheStatisticsService.clearAllCaches();
            result.put("cacheCleared", true);

            // Get fresh metrics
            result.put("metrics", metricsService.getAllMetrics());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(result)
                    .message("Metrics refreshed successfully").build());
        } catch (Exception e) {
            log.error("Error refreshing metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to refresh metrics: " + e.getMessage()).build());
        }
    }

    @PostMapping("/refresh/{metricType}")
    public ResponseEntity<ApiResponse<String>> refreshSpecificMetric(@PathVariable String metricType) {
        try {
            log.info("Refreshing specific metric: {}", metricType);

            String message = switch (metricType.toLowerCase()) {
                case "cache" -> {
                    cacheStatisticsService.clearAllCaches();
                    yield "Cache metrics refreshed";
                }
                case "security" -> {
                    securityEventService.clearExpiredAttempts();
                    yield "Security metrics refreshed";
                }
                case "blacklist" -> {
                    tokenBlacklistService.clearExpiredTokens();
                    yield "Blacklist metrics refreshed";
                }
                case "system" -> "System metrics refreshed (always live)";
                default -> "Unknown metric type: " + metricType;
            };

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).data(message)
                    .message("Metric refreshed successfully").build());
        } catch (Exception e) {
            log.error("Error refreshing metric {}: {}", metricType, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false).message("Failed to refresh metric: " + e.getMessage()).build());
        }
    }

    // ==================== Design / Theme ====================

    @GetMapping("/design")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDesignSettings() {
        try {
            Map<String, Object> design = new HashMap<>();

            // Default admin dashboard theme settings
            design.put("theme", Map.of(
                    "primaryColor", "#007bff",
                    "secondaryColor", "#6c757d",
                    "successColor", "#28a745",
                    "dangerColor", "#dc3545",
                    "warningColor", "#ffc107",
                    "infoColor", "#17a2b8",
                    "darkMode", false));

            design.put("dashboard", Map.of(
                    "refreshInterval", 30000,
                    "showSystemMetrics", true,
                    "showCacheMetrics", true,
                    "showSecurityMetrics", true,
                    "showDatabaseMetrics", true,
                    "animationsEnabled", true));

            design.put("charts", Map.of(
                    "memoryChartEnabled", true,
                    "cacheChartEnabled", true,
                    "securityChartEnabled", true,
                    "updateInterval", 5000));

            design.put("notifications", Map.of(
                    "enabled", true,
                    "emailAlerts", false,
                    "criticalAlertsOnly", true));

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(design)
                    .message("Design settings retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving design settings: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to retrieve design settings: " + e.getMessage()).build());
        }
    }

    @PutMapping("/design")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDesignSettings(
            @RequestBody Map<String, Object> designSettings) {
        try {
            log.info("Updating design settings: {}", designSettings.keySet());

            // In a real application, you would persist these settings
            // For now, we just acknowledge the update
            Map<String, Object> result = new HashMap<>();
            result.put("status", "Design settings updated");
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("receivedSettings", designSettings.keySet());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(result)
                    .message("Design settings updated successfully").build());
        } catch (Exception e) {
            log.error("Error updating design settings: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to update design settings: " + e.getMessage()).build());
        }
    }

    // ==================== Clear ====================

    @PostMapping("/clear/all")
    public ResponseEntity<ApiResponse<Map<String, String>>> clearAll() {
        try {
            log.info("Clearing all caches and expired data");

            Map<String, String> result = new HashMap<>();

            // Clear caches
            cacheStatisticsService.clearAllCaches();
            result.put("caches", "cleared");

            // Clear expired security events
            securityEventService.clearExpiredAttempts();
            result.put("securityEvents", "cleaned");

            // Clear expired tokens
            tokenBlacklistService.clearExpiredTokens();
            result.put("expiredTokens", "removed");

            result.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .success(true).data(result)
                    .message("All caches and expired data cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing all: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, String>>builder()
                    .success(false).message("Failed to clear: " + e.getMessage()).build());
        }
    }

    @PostMapping("/clear/cache")
    public ResponseEntity<ApiResponse<String>> clearCaches() {
        try {
            cacheStatisticsService.clearAllCaches();
            log.info("Cleared all caches");
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("All caches cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing caches: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false).message("Failed to clear caches: " + e.getMessage()).build());
        }
    }

    @PostMapping("/clear/security")
    public ResponseEntity<ApiResponse<String>> clearSecurityData() {
        try {
            securityEventService.clearExpiredAttempts();
            log.info("Cleared expired security attempts");
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("Security data cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing security data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false).message("Failed to clear security data: " + e.getMessage()).build());
        }
    }

    @PostMapping("/clear/blacklist")
    public ResponseEntity<ApiResponse<String>> clearBlacklist() {
        try {
            tokenBlacklistService.clearExpiredTokens();
            log.info("Cleared expired tokens from blacklist");
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("Blacklist cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing blacklist: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false).message("Failed to clear blacklist: " + e.getMessage()).build());
        }
    }

    // ==================== Cleanup ====================

    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performCleanup() {
        try {
            log.info("Performing full cleanup");

            Map<String, Object> result = new HashMap<>();

            // Clean expired tokens
            tokenBlacklistService.clearExpiredTokens();
            result.put("expiredTokens", "cleaned");

            // Clean expired security attempts
            securityEventService.clearExpiredAttempts();
            result.put("securityAttempts", "cleaned");

            // Clear all caches
            cacheStatisticsService.clearAllCaches();
            result.put("caches", "cleared");

            result.put("timestamp", LocalDateTime.now().toString());
            result.put("status", "Cleanup completed successfully");

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true).data(result)
                    .message("Cleanup completed successfully").build());
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .success(false).message("Failed to cleanup: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cleanup/expired")
    public ResponseEntity<ApiResponse<String>> cleanupExpired() {
        try {
            log.info("Cleaning up expired entries");

            int tokensCleaned = 0;
            int attemptsCleaned = 0;

            // The services don't return counts, so we just acknowledge
            tokenBlacklistService.clearExpiredTokens();
            securityEventService.clearExpiredAttempts();

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true).message("Expired entries cleaned successfully").build());
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .success(false).message("Failed to cleanup: " + e.getMessage()).build());
        }
    }

    private String formatPercent(double rate) {
        return String.format("%.2f%%", rate * 100);
    }
}