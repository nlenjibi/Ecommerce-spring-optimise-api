# Smart E-Commerce System - Performance Optimization Report

## Project Overview

**Project Phase:** Advanced Optimization  
**Date:** March 2026  
**Framework:** Spring Boot 3.5.9  
**Java Version:** Java 21  
**Profiling Tools:** VisualVM 2.2.1, JUnit Tests

---

## Executive Summary

This report documents the performance optimization implementation for the Smart E-Commerce System backend. The project addressed critical performance bottlenecks identified through VisualVM profiling and implemented modern Java asynchronous programming patterns, concurrency utilities, and algorithmic optimizations.

### Key Achievements

| Category | Achievement |
|---------|-------------|
| Asynchronous Programming | Implemented @Async, CompletableFuture, ExecutorService |
| Thread Safety | ConcurrentHashMap, LongAdder, StampedLock |
| Parallel Processing | parallelStream() for bulk operations |
| Database Optimization | Hibernate batch fetching, composite indexes |
| Caching | JWT token caching, Caffeine cache |
| Memory Optimization | Jackson configuration, object pooling |

---

## 1. Performance Bottleneck Analysis

### 1.1 Baseline Metrics (Before Optimization)

Based on VisualVM profiling data collected over 31-34 minutes runtime:

#### JDBC Profile
| Query | Time | % of Total | Issue |
|-------|------|------------|-------|
| Product listing with sorting | 11.6 ms | 50.3% | Missing composite index |
| Discount product query | 3.44 ms | 14.9% | Full table scan |
| Product count query | 2.83 ms | 12.2% | Can be cached |
| N+1 image queries | 4+ queries | - | Lazy loading without batch |

#### Memory Profile
| Component | Memory | % | Root Cause |
|-----------|--------|---|------------|
| Filler Arrays | 54.1 MB | 21.1% | Memory fragmentation |
| byte[] | 47.1 MB | 18.4% | JSON/JWT buffers |
| String | 9.48 MB | 3.7% | DTO conversions |
| ConcurrentHashMap | 6.2 MB | 2.4% | Session/cache data |

#### Thread Contention
| Metric | Value |
|--------|-------|
| Lock Object | java.lang.Object |
| Holder Thread | http-nio-9190-exec-2 |
| Blocked Threads | exec-7, Poller |
| Total Wait Time | 3,675 ms (56.3%) |

---

## 2. Asynchronous Programming Implementation

### 2.1 Epic: Async Programming (User Story 2.1, 2.2)

**Objective:** Implement non-blocking request handling using CompletableFuture, ExecutorService, and parallel streams.

### 2.2 Implementation Details

#### A. Async Configuration (AsyncConfig.java)

```java
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(25);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

#### B. Async Inventory Service

New service for background inventory operations triggered by order status changes:

```java
@Service
@RequiredArgsConstructor
public class AsyncInventoryServiceImpl implements AsyncInventoryService {

    @Override
    @Async("taskExecutor")
    public void updateInventoryOnOrderConfirm(Long orderId) {
        // Reduce stock when order is confirmed/processed
    }

    @Override
    @Async("taskExecutor")
    public void updateInventoryOnOrderCancel(Long orderId) {
        // Restore stock when order is cancelled
    }

    @Override
    @Async("taskExecutor")
    public void updateInventoryOnOrderDeliver(Long orderId) {
        // Update sales count when order is delivered
    }
}
```

#### C. Integration with OrderService

Order status changes now trigger async inventory updates:

- `processOrder()` → triggers `updateInventoryOnOrderConfirm()`
- `cancelOrder()` → triggers `updateInventoryOnOrderCancel()`
- `deliverOrder()` → triggers `updateInventoryOnOrderDeliver()`

### 2.3 Test Results

```
Sequential time: 1730ms
Parallel time: 194ms
Speedup: ~9x faster
```

---

## 3. Concurrency and Thread Safety

### 3.1 Epic: Thread Safety (User Story 3.1, 3.2)

**Objective:** Use thread-safe data structures and balance concurrency levels.

### 3.2 Implementation Details

#### A. LongAdder for Counters (RateLimitingAspect.java)

```java
private static class RequestCounter {
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder requestsLastMinute = new LongAdder();
    private final LongAdder requestsLastSecond = new LongAdder();
}
```

#### B. StampedLock for Access Log (SecurityEventService.java)

```java
private final StampedLock accessLogLock = new StampedLock();

public void addAccessLogEntry(AccessLogEntry entry) {
    long stamp = accessLogLock.writeLock();
    try {
        accessLog.put(key, entry);
    } finally {
        accessLogLock.unlockWrite(stamp);
    }
}
```

#### C. ConcurrentHashMap Usage

- `MetricsService` - Custom metrics storage
- `SecurityEventService` - Access log storage
- `RateLimitingAspect` - Request counters
- `QueryPerformanceAspect` - Query statistics

### 3.3 Thread Pool Configuration

| Pool | Core | Max | Queue | Purpose |
|------|------|-----|-------|---------|
| taskExecutor | 10 | 25 | 100 | General async operations |

---

## 4. Data and Algorithmic Optimization

### 4.1 Epic: DSA Optimization (User Story 4.1, 4.2)

**Objective:** Improve data access efficiency using sorting, searching, and caching.

### 4.2 Parallel Stream Implementation

#### Bulk Price Update with Parallel Streams

```java
@Override
@Transactional
public void bulkUpdatePrices(List<Long> productIds, BigDecimal priceChange, boolean isPercentage) {
    List<Product> products = productRepository.findByIdIn(productIds);
    
    products.parallelStream().forEach(product -> {
        BigDecimal currentPrice = product.getPrice();
        BigDecimal newPrice;
        
        if (isPercentage) {
            newPrice = currentPrice.multiply(
                BigDecimal.ONE.add(priceChange.divide(BigDecimal.valueOf(100))));
        } else {
            newPrice = currentPrice.add(priceChange);
        }
        
        if (newPrice.compareTo(BigDecimal.ZERO) > 0) {
            product.setPrice(newPrice);
        }
    });
    
    productRepository.saveAll(products);
}
```

#### Async Statistics Recalculation

```java
@Async("taskExecutor")
public void recalculateAllProductStatsAsync() {
    List<Product> products = productRepository.findAll();
    
    products.parallelStream().forEach(product -> {
        product.updateInventoryStatus();
    });
    
    productRepository.saveAll(products);
}
```

### 4.3 Performance Comparison

| Metric | Before | After |
|--------|--------|-------|
| Bulk price update | Sequential | Parallel (multi-core) |
| Stats recalculation | Sequential | Parallel |
| Time complexity | O(n) | O(n/p) where p = cores |

---

## 5. Metrics Collection and Reporting

### 5.1 Epic: Metrics (User Story 5.1, 5.2)

**Objective:** Collect runtime metrics and document performance improvements.

### 5.2 Cache Statistics

| Cache Name | Max Size | TTL | Purpose |
|------------|----------|-----|---------|
| products-page | 100 | 1 min | Product listings |
| products-trending | 100 | 1 min | Trending products |
| products-discounted | 100 | 1 min | Discounted products |
| userPrincipals | 10,000 | - | User details |
| tokenValidation | 10,000 | 10 min | JWT tokens |

### 5.3 Database Connection Pool (HikariCP)

| Setting | Value |
|---------|-------|
| Maximum Pool Size | 20 |
| Minimum Idle | 10 |
| Connection Timeout | 30s |
| Idle Timeout | 10min |
| Max Lifetime | 30min |



## 9. Conclusion

This optimization project successfully addressed critical performance bottlenecks through:

1. **Asynchronous Programming:** Implemented @Async and ExecutorService for non-blocking operations
2. **Thread Safety:** Used ConcurrentHashMap, LongAdder, and StampedLock for thread-safe operations
3. **Parallel Processing:** Leveraged parallelStream() for bulk operations
4. **Database Optimization:** Hibernate batch fetching and composite indexes
5. **Caching:** JWT token caching and Caffeine cache
6. **Memory Optimization:** Jackson configuration


