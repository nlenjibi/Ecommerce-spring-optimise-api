package com.smart_ecomernce_api.smart_ecomernce_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency Test - Simple Unit Tests
 * 
 * Tests thread safety without Spring context
 */
@DisplayName("Concurrency Tests")
class ConcurrencyTest {

    @Test
    @DisplayName("Test LongAdder concurrent increment")
    void testLongAdderConcurrentIncrement() throws InterruptedException {
        int threadCount = 50;
        int incrementsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        LongAdder longAdder = new LongAdder();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        longAdder.increment();
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail("Exception during concurrent increment: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(threadCount * incrementsPerThread, successCount.get());
        assertEquals(threadCount * incrementsPerThread, longAdder.sum());
    }

    @Test
    @DisplayName("Test ConcurrentHashMap thread safety")
    void testConcurrentHashMapThreadSafety() throws InterruptedException {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        int threadCount = 20;
        int opsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < opsPerThread; j++) {
                        String key = "key-" + threadId + "-" + j;
                        map.put(key, j);
                        map.get(key);
                        map.containsKey(key);
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
        
        assertTrue(map.size() > 0, "Map should contain data");
    }

    @Test
    @DisplayName("Test thread pool executor shutdown")
    void testThreadPoolExecutorShutdown() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // Submit tasks
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
        boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);
        
        assertTrue(terminated, "Executor should terminate properly");
    }

    @Test
    @DisplayName("Test no race conditions in concurrent operations")
    void testNoRaceConditions() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 10;
        int iterations = 10000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        
        // Multiple threads increment
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        counter.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail("Error: " + e.getMessage());
                }
            });
        }
        
        // Multiple threads read
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        int value = counter.get();
                        assertTrue(value >= 0);
                    }
                } catch (Exception e) {
                    fail("Error: " + e.getMessage());
                }
            });
        }
        
        startLatch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
        
        // Final value should be exactly threadCount * iterations
        assertEquals(threadCount * iterations, counter.get());
    }

    @Test
    @DisplayName("Test CopyOnWriteArrayList thread safety")
    void testCopyOnWriteArrayList() throws InterruptedException {
        java.util.List<String> list = new java.util.concurrent.CopyOnWriteArrayList<>();
        int threadCount = 10;
        int opsPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < opsPerThread; j++) {
                        list.add("item-" + threadId + "-" + j);
                        list.contains("item-" + threadId + "-" + j);
                    }
                } catch (Exception e) {
                    fail("Error: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        assertTrue(endLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertEquals(threadCount * opsPerThread, list.size());
    }
}
