package com.smart_ecomernce_api.smart_ecomernce_api.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * GraphQL Caching Aspect
 * 
 * Monitors and logs cache operations for GraphQL queries and mutations.
 * Provides visibility into cache performance and helps optimize caching strategy.
 * 
 * Features:
 * - Logs cache hits/misses for @Cacheable annotated methods
 * - Logs cache evictions for @CacheEvict annotated methods
 * - Tracks execution time for cached vs non-cached operations
 * - Provides performance insights for GraphQL operations
 * 
 * Works with:
 * - @QueryMapping (read operations - typically cached)
 * - @MutationMapping (write operations - typically evict cache)
 */
@Aspect
@Component
@Slf4j
public class GraphQLCachingAspect {

    /**
     * Pointcut for GraphQL Query mappings
     */
    @Pointcut("@annotation(org.springframework.graphql.data.method.annotation.QueryMapping)")
    public void queryMappingPointcut() {}

    /**
     * Pointcut for GraphQL Mutation mappings
     */
    @Pointcut("@annotation(org.springframework.graphql.data.method.annotation.MutationMapping)")
    public void mutationMappingPointcut() {}

    /**
     * Pointcut for methods annotated with @Cacheable
     */
    @Pointcut("@annotation(org.springframework.cache.annotation.Cacheable)")
    public void cacheablePointcut() {}

    /**
     * Pointcut for methods annotated with @CacheEvict
     */
    @Pointcut("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public void cacheEvictPointcut() {}

    /**
     * Monitor cacheable GraphQL queries
     * Logs cache configuration and execution time
     */
    @Around("queryMappingPointcut() && cacheablePointcut()")
    public Object monitorCacheableQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        
        String methodName = method.getName();
        String[] cacheNames = cacheable.value();
        String cacheKey = cacheable.key();
        Object[] args = joinPoint.getArgs();

        log.info("🔍 GraphQL Query: {} - Cache lookup", methodName);
        log.debug("   Cache names: {}", Arrays.toString(cacheNames));
        log.debug("   Cache key: {}", cacheKey);
        log.debug("   Arguments: {}", formatArguments(args));

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean cacheHit = false;

        try {
            result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // If execution is very fast, it's likely a cache hit
            cacheHit = executionTime < 10;

            if (cacheHit) {
                log.info("✅ GraphQL Query: {} - CACHE HIT ({} ms)", methodName, executionTime);
            } else {
                log.info("💾 GraphQL Query: {} - CACHE MISS, fetched from DB ({} ms)", methodName, executionTime);
            }

            return result;

        } catch (Throwable ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ GraphQL Query: {} - FAILED ({} ms)", methodName, executionTime);
            throw ex;
        }
    }

    /**
     * Monitor cache eviction for GraphQL mutations
     * Logs which caches are being cleared
     */
    @Around("mutationMappingPointcut() && cacheEvictPointcut()")
    public Object monitorCacheEvictMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
        
        String methodName = method.getName();
        String[] cacheNames = cacheEvict.value();
        boolean allEntries = cacheEvict.allEntries();
        Object[] args = joinPoint.getArgs();

        log.info("🔄 GraphQL Mutation: {} - Preparing cache eviction", methodName);
        log.debug("   Cache names: {}", Arrays.toString(cacheNames));
        log.debug("   Clear all entries: {}", allEntries);
        log.debug("   Arguments: {}", formatArguments(args));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (allEntries) {
                log.warn("🗑️  GraphQL Mutation: {} - CACHE CLEARED (all entries in {}) - {} ms", 
                    methodName, Arrays.toString(cacheNames), executionTime);
            } else {
                log.info("🗑️  GraphQL Mutation: {} - CACHE EVICTED (specific entries) - {} ms", 
                    methodName, executionTime);
            }

            return result;

        } catch (Throwable ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ GraphQL Mutation: {} - FAILED, cache eviction may be incomplete ({} ms)", 
                methodName, executionTime);
            throw ex;
        }
    }

    /**
     * Monitor GraphQL queries without caching (to identify optimization opportunities)
     */
    @Around("queryMappingPointcut() && !cacheablePointcut()")
    public Object monitorUncachedQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        log.debug("⚠️  GraphQL Query: {} - NOT CACHED (consider adding @Cacheable)", methodName);

        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 100) {
                log.warn("⚠️  GraphQL Query: {} - Slow uncached query ({} ms) - Consider caching", 
                    methodName, executionTime);
            }

            return result;

        } catch (Throwable ex) {
            throw ex;
        }
    }

    /**
     * Monitor GraphQL mutations without cache eviction
     */
    @Around("mutationMappingPointcut() && !cacheEvictPointcut()")
    public Object monitorUnevictedMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        log.debug("ℹ️  GraphQL Mutation: {} - No cache eviction configured", methodName);

        return joinPoint.proceed();
    }

    /**
     * Format arguments for logging (sanitize sensitive data)
     */
    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "none";
        }

        return Arrays.stream(args)
            .map(arg -> {
                if (arg == null) return "null";
                
                // Sanitize sensitive data
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
}
