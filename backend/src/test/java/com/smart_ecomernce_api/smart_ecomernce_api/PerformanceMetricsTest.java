package com.smart_ecomernce_api.smart_ecomernce_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance Metrics Test - Simple Unit Tests
 * 
 * Tests without Spring context to avoid database dependency
 */
@DisplayName("Performance Metrics Tests")
class PerformanceMetricsTest {

    @Test
    @DisplayName("Test concurrent cache-like operations performance")
    void testConcurrentCachePerformance() throws InterruptedException {
        ConcurrentHashMap<String, Integer> cache = new ConcurrentHashMap<>();
        int threadCount = 10;
        int operationsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key-" + (threadId * operationsPerThread + j);
                        cache.put(key, j);
                        cache.get(key);
                    }
                } catch (Exception e) {
                    fail("Exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue(cache.size() > 0, "Cache should contain data");
    }

    @Test
    @DisplayName("Test LongAdder performance vs AtomicInteger")
    void testLongAdderPerformance() throws InterruptedException {
        int iterations = 100000;
        
        // Test LongAdder
        LongAdder longAdder = new LongAdder();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        long startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    longAdder.increment();
                }
                latch.countDown();
            });
        }
        latch.await();
        long longAdderTime = System.nanoTime() - startTime;

        // Test AtomicInteger
        AtomicInteger atomicInteger = new AtomicInteger();
        CountDownLatch latch2 = new CountDownLatch(10);

        startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    atomicInteger.incrementAndGet();
                }
                latch2.countDown();
            });
        }
        latch2.await();
        long atomicTime = System.nanoTime() - startTime;

        executor.shutdown();

        assertEquals(10 * iterations, longAdder.sum());
        assertEquals(10 * iterations, atomicInteger.get());
        
        // LongAdder should be faster or comparable under contention
        System.out.println("LongAdder: " + longAdderTime + "ns, AtomicInteger: " + atomicTime + "ns");
    }

    @Test
    @DisplayName("Test StampedLock read-write performance")
    void testStampedLockPerformance() throws InterruptedException {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        java.util.concurrent.locks.StampedLock lock = new java.util.concurrent.locks.StampedLock();
        int iterations = 10000;

        // Test concurrent reads with optimistic locking
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(20);

        // Populate map
        for (int i = 0; i < 100; i++) {
            map.put("key" + i, i);
        }

        long startTime = System.nanoTime();

        // Readers (15 threads)
        for (int i = 0; i < 15; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        long stamp = lock.tryOptimisticRead();
                        int value = map.get("key" + (j % 100));
                        if (!lock.validate(stamp)) {
                            stamp = lock.readLock();
                            try {
                                value = map.get("key" + (j % 100));
                            } finally {
                                lock.unlockRead(stamp);
                            }
                        }
                    }
                } catch (Exception e) {
                    fail("Read error: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Writers (5 threads)
        for (int i = 0; i < 5; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations / 10; j++) {
                        long stamp = lock.writeLock();
                        try {
                            map.put("key" + (writerId * 100 + j % 100), j);
                        } finally {
                            lock.unlockWrite(stamp);
                        }
                    }
                } catch (Exception e) {
                    fail("Write error: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(60, TimeUnit.SECONDS));
        long duration = System.nanoTime() - startTime;

        executor.shutdown();

        System.out.println("StampedLock test completed in: " + duration + "ns");
        assertTrue(duration < 5_000_000_000L, "Should complete within 5 seconds");
    }

    @Test
    @DisplayName("Test Caffeine-like cache eviction")
    void testCacheEviction() {
        ConcurrentHashMap<String, Integer> cache = new ConcurrentHashMap<>();
        int maxSize = 100;
        
        for (int i = 0; i < maxSize + 50; i++) {
            cache.put("token-" + i, i);
            if (cache.size() > maxSize) {
                // Simple eviction - remove oldest
                String oldestKey = cache.keys().nextElement();
                cache.remove(oldestKey);
            }
        }
        
        assertTrue(cache.size() <= maxSize + 10, "Cache should be bounded");
    }
}
