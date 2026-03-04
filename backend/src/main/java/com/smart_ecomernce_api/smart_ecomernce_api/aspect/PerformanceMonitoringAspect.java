package com.smart_ecomernce_api.smart_ecomernce_api.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Performance Monitoring Aspect
 *
 * Monitors and logs execution time for all service methods.
 * Provides performance insights and identifies slow operations.
 *
 * Performance Thresholds:
 * - FAST: < 100ms (DEBUG level)
 * - NORMAL: 100-500ms (INFO level)
 * - SLOW: 500-1000ms (WARN level)
 * - VERY SLOW: > 1000ms (ERROR level - needs optimization)
 */
@Aspect
@Component
@Slf4j
public class PerformanceMonitoringAspect {

    // Performance thresholds in milliseconds
    private static final long FAST_THRESHOLD = 100;
    private static final long NORMAL_THRESHOLD = 500;
    private static final long SLOW_THRESHOLD = 1000;

    /**
     * Pointcut for all service methods across all modules
     */
    @Pointcut("execution(* com.smart_ecomernce_api.Smart_ecommerce_api.modules..service..*.*(..))")
    public void serviceMethodsPointcut() {}

    /**
     * Around advice - measures execution time of service methods
     * Wraps the method execution and calculates the time taken
     *
     * @param joinPoint The join point representing the method execution
     * @return The result of the method execution
     * @throws Throwable if the method throws an exception
     */
    @Around("serviceMethodsPointcut()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean methodFailed = false;

        try {
            // Execute the actual method
            result = joinPoint.proceed();
            return result;

        } catch (Throwable ex) {
            methodFailed = true;
            throw ex;

        } finally {
            // Calculate execution time
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Log performance metrics
            if (methodFailed) {
                log.error("⚠️ PERFORMANCE: {} FAILED after {} ms", fullMethodName, executionTime);
            } else {
                logPerformance(fullMethodName, executionTime);
            }
        }
    }

    /**
     * Helper method to log performance metrics with appropriate level
     * based on execution time thresholds
     *
     * @param methodName The full method name (ClassName.methodName)
     * @param executionTime The execution time in milliseconds
     */
    private void logPerformance(String methodName, long executionTime) {
        String performanceEmoji;
        String performanceLabel;

        if (executionTime < FAST_THRESHOLD) {
            performanceEmoji = "⚡";
            performanceLabel = "FAST";
            log.debug("{} PERFORMANCE: {} executed in {} ms [{}]",
                    performanceEmoji, methodName, executionTime, performanceLabel);

        } else if (executionTime < NORMAL_THRESHOLD) {
            performanceEmoji = "✓";
            performanceLabel = "NORMAL";
            log.info("{} PERFORMANCE: {} executed in {} ms [{}]",
                    performanceEmoji, methodName, executionTime, performanceLabel);

        } else if (executionTime < SLOW_THRESHOLD) {
            performanceEmoji = "⚠️";
            performanceLabel = "SLOW";
            log.warn("{} PERFORMANCE: {} executed in {} ms [{}] - Consider optimization",
                    performanceEmoji, methodName, executionTime, performanceLabel);

        } else {
            performanceEmoji = "🐌";
            performanceLabel = "VERY SLOW";
            log.error("{} PERFORMANCE: {} executed in {} ms [{}] - URGENT: Optimization needed!",
                    performanceEmoji, methodName, executionTime, performanceLabel);
        }
    }

    /**
     * Get performance summary statistics
     * Can be extended to track performance metrics over time
     */
    private String getPerformanceSummary(long executionTime) {
        if (executionTime < FAST_THRESHOLD) return "Excellent performance";
        if (executionTime < NORMAL_THRESHOLD) return "Acceptable performance";
        if (executionTime < SLOW_THRESHOLD) return "Performance degradation detected";
        return "Critical performance issue";
    }
}
