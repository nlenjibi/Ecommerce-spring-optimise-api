package com.smart_ecomernce_api.smart_ecomernce_api.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Logging Aspect
 *
 * Provides comprehensive logging for all service layer methods across all modules.
 * Logs method entry, exit, and exceptions with detailed information.
 *
 * Applies to: All service classes in com.smart_ecomernce_api.Smart_ecommerce_api.modules.*.service
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Pointcut for all service methods across all modules
     * Matches: com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.service.*
     *          com.smart_ecomernce_api.Smart_ecommerce_api.modules.product.service.*
     *          com.smart_ecomernce_api.Smart_ecommerce_api.modules.order.service.*
     *          etc.
     */
    @Pointcut("execution(* com.smart_ecomernce_api.Smart_ecommerce_api.modules..service..*.*(..))")
    public void serviceMethodsPointcut() {}

    /**
     * Before advice - logs method entry with parameters
     * Executes before the target method is called
     */
    @Before("serviceMethodsPointcut()")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        log.info(">>> ENTERING: {}.{}", className, methodName);

        if (args != null && args.length > 0) {
            String argsString = Arrays.stream(args)
                    .map(arg -> arg != null ? arg.getClass().getSimpleName() : "null")
                    .collect(Collectors.joining(", "));
            log.debug("    Parameters: [{}]", argsString);
            log.trace("    Values: {}", Arrays.toString(args));
        } else {
            log.debug("    Parameters: none");
        }
    }

    /**
     * AfterReturning advice - logs successful method execution with return value
     * Executes after the target method returns successfully
     */
    @AfterReturning(pointcut = "serviceMethodsPointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.info("<<< EXITING: {}.{} - SUCCESS", className, methodName);

        if (result != null) {
            log.debug("    Return type: {}", result.getClass().getSimpleName());
            log.trace("    Return value: {}", result);
        } else {
            log.debug("    Return value: null");
        }
    }

    /**
     * AfterThrowing advice - logs exceptions thrown by service methods
     * Executes when the target method throws an exception
     */
    @AfterThrowing(pointcut = "serviceMethodsPointcut()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String exceptionName = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();

        log.error("<<< EXITING: {}.{} - EXCEPTION THROWN", className, methodName);
        log.error("    Exception: {}", exceptionName);
        log.error("    Message: {}", exceptionMessage != null ? exceptionMessage : "No message");

        // Only log full stack trace at TRACE level to avoid cluttering logs
        if (log.isTraceEnabled()) {
            log.trace("    Stack trace:", exception);
        }
    }

    /**
     * After advice - logs method completion regardless of outcome
     * Executes after the target method completes (success or failure)
     */
    @After("serviceMethodsPointcut()")
    public void logAfter(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.trace("Method {}.{} execution completed", className, methodName);
    }
}
