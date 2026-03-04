package com.smart_ecomernce_api.smart_ecomernce_api.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception Handling Aspect
 * 
 * Provides centralized exception handling and error tracking across all layers.
 * Monitors exception patterns, tracks error frequencies, and provides detailed error context.
 * 
 * Features:
 * - Categorizes exceptions by type (Business, Technical, Security, Validation)
 * - Tracks exception frequency and patterns
 * - Provides detailed error context for debugging
 * - Identifies recurring errors that need attention
 * - Supports root cause analysis
 */
@Aspect
@Component
@Slf4j
public class ExceptionHandlingAspect {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Track exception frequency for pattern detection
    private final Map<String, Integer> exceptionFrequency = new HashMap<>();
    private static final int FREQUENCY_THRESHOLD = 5;

    /**
     * Pointcut for all service layer methods
     */
    @Pointcut("execution(* com.smart_ecomernce_api.Smart_ecommerce_api.modules..service..*.*(..))")
    public void serviceLayerPointcut() {}

    /**
     * Pointcut for all controller layer methods
     */
    @Pointcut("execution(* com.smart_ecomernce_api.Smart_ecommerce_api.modules..controller..*.*(..))")
    public void controllerLayerPointcut() {}

    /**
     * Pointcut for all repository layer methods
     */
    @Pointcut("execution(* com.smart_ecomernce_api.Smart_ecommerce_api.modules..repository..*.*(..))")
    public void repositoryLayerPointcut() {}

    /**
     * Handle service layer exceptions
     */
    @AfterThrowing(pointcut = "serviceLayerPointcut()", throwing = "exception")
    public void handleServiceException(JoinPoint joinPoint, Throwable exception) {
        handleException(joinPoint, exception, "SERVICE");
    }

    /**
     * Handle controller layer exceptions
     */
    @AfterThrowing(pointcut = "controllerLayerPointcut()", throwing = "exception")
    public void handleControllerException(JoinPoint joinPoint, Throwable exception) {
        handleException(joinPoint, exception, "CONTROLLER");
    }

    /**
     * Handle repository layer exceptions
     */
    @AfterThrowing(pointcut = "repositoryLayerPointcut()", throwing = "exception")
    public void handleRepositoryException(JoinPoint joinPoint, Throwable exception) {
        handleException(joinPoint, exception, "REPOSITORY");
    }

    /**
     * Central exception handling logic
     */
    private void handleException(JoinPoint joinPoint, Throwable exception, String layer) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String fullMethodName = className + "." + methodName;
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        // Categorize exception
        ExceptionCategory category = categorizeException(exception);
        
        // Track exception frequency
        String exceptionKey = exception.getClass().getSimpleName() + "@" + fullMethodName;
        int frequency = exceptionFrequency.merge(exceptionKey, 1, Integer::sum);
        
        // Build detailed error log
        StringBuilder errorLog = new StringBuilder();
        errorLog.append("\n╔═══════════════════════════════════════════════════════════════");
        errorLog.append("\n║ ❌ EXCEPTION DETECTED - ").append(category.getEmoji()).append(" ").append(category.name());
        errorLog.append("\n╠═══════════════════════════════════════════════════════════════");
        errorLog.append("\n║ Timestamp:       ").append(timestamp);
        errorLog.append("\n║ Layer:           ").append(layer);
        errorLog.append("\n║ Class:           ").append(className);
        errorLog.append("\n║ Method:          ").append(methodName);
        errorLog.append("\n║ Exception Type:  ").append(exception.getClass().getSimpleName());
        errorLog.append("\n║ Message:         ").append(exception.getMessage() != null ? exception.getMessage() : "No message");
        errorLog.append("\n║ Frequency:       ").append(frequency).append(" occurrence(s)");
        
        // Add warning if exception is recurring
        if (frequency >= FREQUENCY_THRESHOLD) {
            errorLog.append("\n║ ⚠️  WARNING:      RECURRING ERROR - Needs immediate attention!");
        }
        
        // Add root cause if available
        Throwable rootCause = getRootCause(exception);
        if (rootCause != exception) {
            errorLog.append("\n║ Root Cause:      ").append(rootCause.getClass().getSimpleName());
            errorLog.append("\n║ Root Message:    ").append(rootCause.getMessage());
        }
        
        // Add method arguments (sanitized)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            errorLog.append("\n║ Arguments:       ").append(formatArguments(args));
        }
        
        errorLog.append("\n╚═══════════════════════════════════════════════════════════════");
        
        // Log based on category severity
        switch (category) {
            case CRITICAL:
            case SECURITY:
                log.error(errorLog.toString(), exception);
                break;
            case BUSINESS:
            case VALIDATION:
                log.warn(errorLog.toString());
                if (log.isDebugEnabled()) {
                    log.debug("Stack trace:", exception);
                }
                break;
            case TECHNICAL:
            default:
                log.error(errorLog.toString());
                if (log.isDebugEnabled()) {
                    log.debug("Stack trace:", exception);
                }
        }
    }

    /**
     * Categorize exception by type
     */
    private ExceptionCategory categorizeException(Throwable exception) {
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        
        // Security exceptions
        if (exceptionName.contains("security") || exceptionName.contains("authentication") ||
            exceptionName.contains("authorization") || exceptionName.contains("access")) {
            return ExceptionCategory.SECURITY;
        }
        
        // Validation exceptions
        if (exceptionName.contains("validation") || exceptionName.contains("constraint") ||
            exceptionName.contains("illegal") || message.contains("invalid")) {
            return ExceptionCategory.VALIDATION;
        }
        
        // Business logic exceptions
        if (exceptionName.contains("business") || exceptionName.contains("notfound") ||
            exceptionName.contains("duplicate") || exceptionName.contains("conflict")) {
            return ExceptionCategory.BUSINESS;
        }
        
        // Critical system exceptions
        if (exceptionName.contains("nullpointer") || exceptionName.contains("outofmemory") ||
            exceptionName.contains("stackoverflow")) {
            return ExceptionCategory.CRITICAL;
        }
        
        // Default to technical
        return ExceptionCategory.TECHNICAL;
    }

    /**
     * Get root cause of exception
     */
    private Throwable getRootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * Format arguments for logging (sanitize sensitive data)
     */
    private String formatArguments(Object[] args) {
        return Arrays.stream(args)
            .map(arg -> {
                if (arg == null) return "null";
                String argString = arg.toString();
                if (argString.toLowerCase().contains("password") || 
                    argString.toLowerCase().contains("token") ||
                    argString.toLowerCase().contains("secret")) {
                    return "[REDACTED]";
                }
                return arg.getClass().getSimpleName();
            })
            .reduce((a, b) -> a + ", " + b)
            .orElse("none");
    }

    /**
     * Exception categories with severity levels
     */
    private enum ExceptionCategory {
        CRITICAL("💥"),      // System-breaking errors
        SECURITY("🔒"),      // Security-related errors
        BUSINESS("💼"),      // Business logic errors
        VALIDATION("✋"),    // Input validation errors
        TECHNICAL("⚙️");     // Technical/infrastructure errors
        
        private final String emoji;
        
        ExceptionCategory(String emoji) {
            this.emoji = emoji;
        }
        
        public String getEmoji() {
            return emoji;
        }
    }

    /**
     * Get exception frequency statistics
     */
    public Map<String, Integer> getExceptionStatistics() {
        return new HashMap<>(exceptionFrequency);
    }

    /**
     * Reset exception frequency tracking
     */
    public void resetStatistics() {
        exceptionFrequency.clear();
        log.info("Exception frequency statistics reset");
    }
}
