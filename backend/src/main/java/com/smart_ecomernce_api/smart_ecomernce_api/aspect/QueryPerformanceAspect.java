package com.smart_ecomernce_api.smart_ecomernce_api.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Query Performance Monitoring Aspect
 *
 * Monitors and tracks JPA query execution performance.
 * Provides insights into slow queries and database performance.
 *
 * Tracks:
 * - Query execution count
 * - Average execution time
 * - Maximum execution time
 * - Slow query detection (> 500ms)
 */
@Aspect
@Component
@Slf4j
public class QueryPerformanceAspect {

    // Query performance tracking
    private final ConcurrentHashMap<String, QueryStats> queryStats = new ConcurrentHashMap<>();

    // Thresholds
    private static final long SLOW_QUERY_THRESHOLD = 500; // ms
    private static final long VERY_SLOW_QUERY_THRESHOLD = 1000; // ms

    /**
     * Pointcut for all repository methods
     */
    @Pointcut("execution(* com.smart_ecomernce_api.smart_ecomernce_api.modules..repository..*.*(..))")
    public void repositoryMethodsPointcut() {}

    /**
     * Pointcut for JPA repository query methods (find*, search*, etc.)
     */
    @Pointcut("execution(* com.smart_ecomernce_api.smart_ecomernce_api.modules..repository..*.find*(..)) || " +
              "execution(* com.smart_ecomernce_api.smart_ecomernce_api.modules..repository..*.search*(..)) || " +
              "execution(* com.smart_ecomernce_api.smart_ecomernce_api.modules..repository..*.get*(..))")
    public void queryMethodsPointcut() {}

    /**
     * Around advice for repository methods
     */
    @Around("repositoryMethodsPointcut()")
    public Object monitorQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String queryKey = className + "." + methodName;

        long startTime = System.nanoTime();
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            // Update statistics
            updateQueryStats(queryKey, executionTimeMs);

            // Log slow queries
            if (executionTimeMs >= SLOW_QUERY_THRESHOLD) {
                logSlowQuery(queryKey, executionTimeMs, joinPoint.getArgs());
            }
        }
    }

    /**
     * Update query statistics
     */
    private void updateQueryStats(String queryKey, long executionTimeMs) {
        queryStats.computeIfAbsent(queryKey, k -> new QueryStats())
                .recordExecution(executionTimeMs);
    }

    /**
     * Log slow query warning
     */
    private void logSlowQuery(String queryKey, long executionTimeMs, Object[] args) {
        StringBuilder argInfo = new StringBuilder();
        if (args != null && args.length > 0) {
            argInfo.append(" Args: [");
            for (int i = 0; i < Math.min(args.length, 3); i++) {
                if (i > 0) argInfo.append(", ");
                argInfo.append(args[i] != null ? args[i].toString() : "null");
            }
            if (args.length > 3) {
                argInfo.append("... (+").append(args.length - 3).append(" more)");
            }
            argInfo.append("]");
        }

        if (executionTimeMs >= VERY_SLOW_QUERY_THRESHOLD) {
            log.error("🐌 VERY SLOW QUERY: {} took {} ms{} - URGENT OPTIMIZATION NEEDED",
                    queryKey, executionTimeMs, argInfo);
        } else {
            log.warn("⚠️ SLOW QUERY: {} took {} ms{} - Consider optimization",
                    queryKey, executionTimeMs, argInfo);
        }
    }

    /**
     * Get query statistics report
     */
    public QueryPerformanceReport generateReport() {
        QueryPerformanceReport report = new QueryPerformanceReport();

        queryStats.forEach((query, stats) -> {
            report.addQueryStat(query, stats);
        });

        return report;
    }


    /**
     * Get slowest queries
     */
    public java.util.List<QueryStatEntry> getSlowestQueries(int limit) {
        return queryStats.entrySet().stream()
                .map(e -> new QueryStatEntry(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.stats.getMaxTime(), a.stats.getMaxTime()))
                .limit(limit)
                .toList();
    }

    /**
     * Inner class for query statistics
     */
    public static class QueryStats {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong slowQueryCount = new AtomicLong(0);

        public void recordExecution(long executionTimeMs) {
            count.incrementAndGet();
            totalTime.addAndGet(executionTimeMs);

            // Update max
            long currentMax;
            do {
                currentMax = maxTime.get();
            } while (executionTimeMs > currentMax && !maxTime.compareAndSet(currentMax, executionTimeMs));

            // Update min
            long currentMin;
            do {
                currentMin = minTime.get();
            } while (executionTimeMs < currentMin && !minTime.compareAndSet(currentMin, executionTimeMs));

            // Count slow queries
            if (executionTimeMs >= SLOW_QUERY_THRESHOLD) {
                slowQueryCount.incrementAndGet();
            }
        }

        public long getCount() { return count.get(); }
        public long getTotalTime() { return totalTime.get(); }
        public long getMaxTime() { return maxTime.get(); }
        public long getMinTime() { return minTime.get() == Long.MAX_VALUE ? 0 : minTime.get(); }
        public long getSlowQueryCount() { return slowQueryCount.get(); }
        public double getAverageTime() { return count.get() > 0 ? (double) totalTime.get() / count.get() : 0; }
    }

    /**
     * Query stat entry for reporting
     */
    public record QueryStatEntry(String queryName, QueryStats stats) {}

    /**
     * Query performance report
     */
    public static class QueryPerformanceReport {
        private final java.util.List<QueryStatDetail> details = new java.util.ArrayList<>();

        public void addQueryStat(String queryName, QueryStats stats) {
            details.add(new QueryStatDetail(
                    queryName,
                    stats.getCount(),
                    stats.getAverageTime(),
                    stats.getMinTime(),
                    stats.getMaxTime(),
                    stats.getSlowQueryCount()
            ));
        }

        public java.util.List<QueryStatDetail> getDetails() { return details; }

        public long getTotalQueries() {
            return details.stream().mapToLong(QueryStatDetail::count).sum();
        }

        public long getTotalSlowQueries() {
            return details.stream().mapToLong(QueryStatDetail::slowCount).sum();
        }
    }

    /**
     * Query stat detail record
     */
    public record QueryStatDetail(
            String queryName,
            long count,
            double avgTime,
            long minTime,
            long maxTime,
            long slowCount
    ) {}
}
