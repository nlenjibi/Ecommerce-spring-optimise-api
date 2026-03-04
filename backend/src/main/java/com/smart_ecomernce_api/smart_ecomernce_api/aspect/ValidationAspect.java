package com.smart_ecomernce_api.smart_ecomernce_api.aspect;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * Validation Aspect
 * 
 * Provides automatic validation for method parameters and input data.
 * Validates objects annotated with Jakarta Bean Validation annotations
 * before method execution.
 * 
 * Features:
 * - Pre-execution validation of input parameters
 * - Detailed validation error logging
 * - Supports @Valid, @Validated annotations
 * - Custom validation for business rules
 * - Early detection of invalid data
 */
@Aspect
@Component
@Slf4j
public class ValidationAspect {

    private final Validator validator;

    public ValidationAspect(Validator validator) {
        this.validator = validator;
    }

    /**
     * Pointcut for service layer methods
     */
    @Pointcut("execution(* com.smart_ecomernce_api.Smart_ecommerce_api.modules..service..*.*(..))")
    public void serviceMethodsPointcut() {}

    /**
     * Pointcut for controller layer methods
     */
    @Pointcut("execution(* com.smart_ecomernce_api.Smart_ecommerce_api.modules..controller..*.*(..))")
    public void controllerMethodsPointcut() {}

    /**
     * Pointcut for methods with parameters annotated with @Valid
     */
    @Pointcut("execution(* *(@org.springframework.validation.annotation.Validated (*), ..))")
    public void validatedParametersPointcut() {}

    /**
     * Validate input parameters before service method execution
     */
    @Before("serviceMethodsPointcut() || controllerMethodsPointcut()")
    public void validateInputParameters(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        if (args == null || args.length == 0) {
            return;
        }

        boolean hasValidationErrors = false;
        StringBuilder validationLog = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            
            // Skip null, primitives, and strings
            if (arg == null || isPrimitiveOrWrapper(arg) || arg instanceof String) {
                continue;
            }

            // Validate the object
            Set<ConstraintViolation<Object>> violations = validator.validate(arg);

            if (!violations.isEmpty()) {
                hasValidationErrors = true;
                
                if (validationLog.length() == 0) {
                    validationLog.append("\n╔═══════════════════════════════════════════════════════");
                    validationLog.append("\n║ ⚠️  VALIDATION ERRORS DETECTED");
                    validationLog.append("\n╠═══════════════════════════════════════════════════════");
                    validationLog.append("\n║ Method: ").append(className).append(".").append(methodName);
                }

                validationLog.append("\n║ Parameter ").append(i + 1).append(" (").append(arg.getClass().getSimpleName()).append("):");
                
                for (ConstraintViolation<Object> violation : violations) {
                    validationLog.append("\n║   • ").append(violation.getPropertyPath())
                                 .append(": ").append(violation.getMessage());
                }
            }
        }

        if (hasValidationErrors) {
            validationLog.append("\n╚═══════════════════════════════════════════════════════");
            log.warn(validationLog.toString());
        } else {
            log.debug("✅ Validation passed for {}.{}", className, methodName);
        }
    }

    /**
     * Validate business rules for specific operations
     */
    @Before("serviceMethodsPointcut()")
    public void validateBusinessRules(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName().toLowerCase();
        Object[] args = joinPoint.getArgs();

        // Custom validation for create operations
        if (methodName.contains("create")) {
            validateCreateOperation(joinPoint, args);
        }

        // Custom validation for update operations
        if (methodName.contains("update")) {
            validateUpdateOperation(joinPoint, args);
        }

        // Custom validation for delete operations
        if (methodName.contains("delete")) {
            validateDeleteOperation(joinPoint, args);
        }
    }

    /**
     * Validate create operations
     */
    private void validateCreateOperation(JoinPoint joinPoint, Object[] args) {
        String methodName = joinPoint.getSignature().getName();
        
        log.debug("🔍 Validating CREATE operation: {}", methodName);
        
        // Check if required parameters are present
        if (args == null || args.length == 0) {
            log.warn("⚠️  CREATE operation {} called with no parameters", methodName);
        }
    }

    /**
     * Validate update operations
     */
    private void validateUpdateOperation(JoinPoint joinPoint, Object[] args) {
        String methodName = joinPoint.getSignature().getName();
        
        log.debug("🔍 Validating UPDATE operation: {}", methodName);
        
        // Check if ID parameter is present
        boolean hasIdParameter = Arrays.stream(args)
            .anyMatch(arg -> arg != null && 
                (arg instanceof Long || arg instanceof Integer || arg instanceof String));
        
        if (!hasIdParameter) {
            log.warn("⚠️  UPDATE operation {} may be missing ID parameter", methodName);
        }
    }

    /**
     * Validate delete operations
     */
    private void validateDeleteOperation(JoinPoint joinPoint, Object[] args) {
        String methodName = joinPoint.getSignature().getName();
        
        log.debug("🔍 Validating DELETE operation: {}", methodName);
        
        // Ensure ID is provided
        if (args == null || args.length == 0) {
            log.error("❌ DELETE operation {} called without ID parameter!", methodName);
        }
    }

    /**
     * Check if object is a primitive or wrapper type
     */
    private boolean isPrimitiveOrWrapper(Object obj) {
        Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() ||
               clazz == Boolean.class ||
               clazz == Byte.class ||
               clazz == Character.class ||
               clazz == Short.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Float.class ||
               clazz == Double.class;
    }
}
