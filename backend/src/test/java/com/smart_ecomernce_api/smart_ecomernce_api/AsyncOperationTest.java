package com.smart_ecomernce_api.smart_ecomernce_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Async Operation Test - Simple Unit Tests
 * 
 * Tests asynchronous programming patterns without Spring context
 */
@DisplayName("Async Operation Tests")
class AsyncOperationTest {

    @Test
    @DisplayName("Test async task executes asynchronously")
    void testAsyncMethodExecutesAsynchronously() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newCachedThreadPool();
        
        // Submit async task
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                counter.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, executor);
        
        // Should return immediately
        assertFalse(future.isDone());
        
        // Wait for completion
        future.get(10, TimeUnit.SECONDS);
        
        assertEquals(1, counter.get());
        executor.shutdown();
    }

    @Test
    @DisplayName("Test CompletableFuture returns value")
    void testCompletableFutureReturnsValue() throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "async-result";
        }, executor);
        
        String result = future.get(10, TimeUnit.SECONDS);
        
        assertNotNull(result);
        assertEquals("async-result", result);
        executor.shutdown();
    }

    @Test
    @DisplayName("Test multiple async tasks run concurrently")
    void testMultipleAsyncTasksRunConynchronously() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        long startTime = System.currentTimeMillis();
        
        // Launch 5 tasks that each take 200ms
        CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> sleep(200), executor);
        CompletableFuture<Void> task2 = CompletableFuture.runAsync(() -> sleep(200), executor);
        CompletableFuture<Void> task3 = CompletableFuture.runAsync(() -> sleep(200), executor);
        CompletableFuture<Void> task4 = CompletableFuture.runAsync(() -> sleep(200), executor);
        CompletableFuture<Void> task5 = CompletableFuture.runAsync(() -> sleep(200), executor);
        
        // Wait for all to complete
        CompletableFuture.allOf(task1, task2, task3, task4, task5).get(10, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // If running truly in parallel, should take ~200ms (plus overhead)
        // If sequential, would take ~1000ms
        assertTrue(duration < 500, "Tasks should run concurrently, took: " + duration + "ms");
        
        executor.shutdown();
    }

    @Test
    @DisplayName("Test async exception handling")
    void testAsyncExceptionHandling() throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Test exception");
        }, executor);
        
        assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    @DisplayName("Test CompletableFuture thenApply chaining")
    void testCompletableFutureChaining() throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        
        String result = CompletableFuture.supplyAsync(() -> "start", executor)
            .thenApply(s -> s + "-processed")
            .thenApply(s -> s + "-completed")
            .get(10, TimeUnit.SECONDS);
        
        assertEquals("start-processed-completed", result);
        executor.shutdown();
    }

    @Test
    @DisplayName("Test async task does not block caller")
    void testAsyncDoesNotBlockCaller() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        ExecutorService executor = Executors.newCachedThreadPool();
        
        long startTime = System.nanoTime();
        
        // This should return immediately
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
                counter.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, executor);
        
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        
        // The method call itself should be fast (< 10ms)
        assertTrue(duration < 10_000_000, "Async call should return quickly, took: " + duration + "ns");
        
        // But the actual work should complete
        future.get(10, TimeUnit.SECONDS);
        assertEquals(1, counter.get());
        
        executor.shutdown();
    }

    @Test
    @DisplayName("Test ThreadPoolTaskExecutor configuration")
    void testThreadPoolConfiguration() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(25);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        
        assertEquals(10, executor.getCorePoolSize());
        assertEquals(25, executor.getMaxPoolSize());
        assertEquals("async-", executor.getThreadNamePrefix());
        
        executor.shutdown();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
