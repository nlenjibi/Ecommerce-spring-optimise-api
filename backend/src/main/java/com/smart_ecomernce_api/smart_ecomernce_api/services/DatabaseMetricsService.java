package com.smart_ecomernce_api.smart_ecomernce_api.services;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting real database performance metrics from HikariCP connection pool
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMetricsService {

    @Autowired(required = false)
    private DataSource dataSource;

    // Query execution time tracking (in milliseconds)
    private final Map<String, AtomicLong> queryExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> queryCounts = new ConcurrentHashMap<>();
    private final List<String> slowQueries = Collections.synchronizedList(new ArrayList<>());
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1 second



    /**
     * Get database metrics from HikariCP connection pool
     */
    public Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Basic database info
        metrics.put("database", getDatabaseInfo());

        // Connection pool metrics
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            metrics.put("connectionPool", getConnectionPoolMetrics(hikariDS));
        } else {
            metrics.put("connectionPool", getDefaultPoolMetrics());
        }

        // Query performance metrics
        metrics.put("queryPerformance", getQueryPerformanceMetrics());

        // Slow queries
        Map<String, Object> slowQueryInfo = new LinkedHashMap<>();
        slowQueryInfo.put("count", slowQueries.size());
        slowQueryInfo.put("threshold", SLOW_QUERY_THRESHOLD_MS + "ms");
        slowQueryInfo.put("recent", slowQueries.stream().limit(10).toList());
        metrics.put("slowQueries", slowQueryInfo);

        // Statistics
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalQueriesTracked", queryCounts.values().stream().mapToLong(AtomicLong::get).sum());
        stats.put("slowQueryCount", slowQueries.size());
        metrics.put("statistics", stats);

        return metrics;
    }

    private Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("url", metaData.getURL());
            info.put("username", metaData.getUserName());
        } catch (SQLException e) {
            log.warn("Could not get database metadata: {}", e.getMessage());
            info.put("error", e.getMessage());
        }
        return info;
    }

    private Map<String, Object> getConnectionPoolMetrics(HikariDataSource hikariDS) {
        Map<String, Object> poolMetrics = new LinkedHashMap<>();
        try {
            HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();

            // Pool configuration
            poolMetrics.put("poolName", hikariDS.getPoolName());
            poolMetrics.put("maximumPoolSize", hikariDS.getMaximumPoolSize());
            poolMetrics.put("minimumIdle", hikariDS.getMinimumIdle());

            // Current pool status
            poolMetrics.put("activeConnections", poolMXBean.getActiveConnections());
            poolMetrics.put("idleConnections", poolMXBean.getIdleConnections());
            poolMetrics.put("totalConnections", poolMXBean.getTotalConnections());
            poolMetrics.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());

            // Connection availability
            int maxPool = hikariDS.getMaximumPoolSize();
            int total = poolMXBean.getTotalConnections();
            double utilization = total > 0 ? ((double) poolMXBean.getActiveConnections() / maxPool) * 100 : 0;
            poolMetrics.put("connectionUtilization", String.format("%.1f%%", utilization));

            // Connection states
            Map<String, Integer> connectionStates = new LinkedHashMap<>();
            connectionStates.put("active", poolMXBean.getActiveConnections());
            connectionStates.put("idle", poolMXBean.getIdleConnections());
            connectionStates.put("waiting", poolMXBean.getThreadsAwaitingConnection());
            poolMetrics.put("connectionStates", connectionStates);

            // Health indicators
            Map<String, String> health = new LinkedHashMap<>();
            if (poolMXBean.getActiveConnections() >= maxPool) {
                health.put("status", "CRITICAL");
                health.put("message", "Pool at maximum capacity!");
            } else if (poolMXBean.getActiveConnections() >= maxPool * 0.8) {
                health.put("status", "WARNING");
                health.put("message", "Pool approaching capacity");
            } else {
                health.put("status", "HEALTHY");
                health.put("message", "Pool operating normally");
            }
            poolMetrics.put("health", health);

        } catch (Exception e) {
            log.warn("Could not get HikariCP metrics: {}", e.getMessage());
            poolMetrics.put("error", e.getMessage());
            poolMetrics.putAll(getDefaultPoolMetrics());
        }
        return poolMetrics;
    }

    private Map<String, Object> getDefaultPoolMetrics() {
        Map<String, Object> defaultMetrics = new LinkedHashMap<>();
        defaultMetrics.put("status", "N/A");
        defaultMetrics.put("message", "HikariCP not configured");
        defaultMetrics.put("activeConnections", 0);
        defaultMetrics.put("idleConnections", 0);
        defaultMetrics.put("totalConnections", 0);
        defaultMetrics.put("maximumPoolSize", "Not configured");
        return defaultMetrics;
    }

    private Map<String, Object> getQueryPerformanceMetrics() {
        Map<String, Object> queryPerf = new LinkedHashMap<>();

        if (queryExecutionTimes.isEmpty()) {
            queryPerf.put("status", "No queries tracked yet");
            return queryPerf;
        }

        // Calculate average execution times per query type
        Map<String, Long> totalTimes = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();

        queryExecutionTimes.forEach((queryType, total) -> {
            totalTimes.put(queryType, total.get());
            counts.put(queryType, queryCounts.get(queryType).get());
        });

        // Average times
        Map<String, Double> avgTimes = new LinkedHashMap<>();
        totalTimes.forEach((queryType, time) -> {
            double avg = (double) time / counts.get(queryType);
            avgTimes.put(queryType, avg);
        });

        queryPerf.put("queryTypesTracked", queryExecutionTimes.keySet().size());
        queryPerf.put("averageExecutionTimes", avgTimes);
        queryPerf.put("totalExecutions", counts);

        // Performance status
        double overallAvg = avgTimes.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Map<String, String> status = new LinkedHashMap<>();
        if (overallAvg < 100) {
            status.put("performance", "EXCELLENT");
            status.put("average", String.format("%.2fms", overallAvg));
        } else if (overallAvg < 500) {
            status.put("performance", "GOOD");
            status.put("average", String.format("%.2fms", overallAvg));
        } else if (overallAvg < 1000) {
            status.put("performance", "FAIR");
            status.put("average", String.format("%.2fms", overallAvg));
        } else {
            status.put("performance", "NEEDS OPTIMIZATION");
            status.put("average", String.format("%.2fms", overallAvg));
        }
        queryPerf.put("status", status);

        return queryPerf;
    }


}
