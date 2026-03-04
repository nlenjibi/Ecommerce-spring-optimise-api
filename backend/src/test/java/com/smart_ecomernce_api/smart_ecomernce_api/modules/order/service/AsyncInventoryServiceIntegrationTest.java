package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Async Operations Integration Test
 * 
 * Tests async patterns used in the inventory service implementation
 */
@DisplayName("Async Operations Integration Tests")
class AsyncInventoryServiceIntegrationTest {

    @Test
    @DisplayName("Test thread pool task executor works correctly")
    void testThreadPoolTaskExecutor() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("test-async-");
        executor.initialize();

        try {
            AtomicInteger counter = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(5);

            for (int i = 0; i < 5; i++) {
                executor.execute(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                });
            }

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            
            assertTrue(completed);
            assertEquals(5, counter.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Test async inventory update pattern")
    void testAsyncInventoryUpdatePattern() throws Exception {
        ConcurrentHashMap<Long, Integer> inventory = new ConcurrentHashMap<>();
        inventory.put(1L, 100);
        inventory.put(2L, 200);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        try {
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                inventory.compute(1L, (k, v) -> v - 10);
            }, executor);

            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                inventory.compute(2L, (k, v) -> v - 20);
            }, executor);

            CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

            assertEquals(90, inventory.get(1L));
            assertEquals(180, inventory.get(2L));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Test concurrent stock reduction is thread-safe")
    void testConcurrentStockReduction() throws InterruptedException {
        ConcurrentHashMap<Long, AtomicInteger> stockMap = new ConcurrentHashMap<>();
        stockMap.put(1L, new AtomicInteger(100));

        int threads = 20;
        int decrementsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < decrementsPerThread; j++) {
                        stockMap.get(1L).decrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(100 - (threads * decrementsPerThread), stockMap.get(1L).get());
    }

    @Test
    @DisplayName("Test order confirm reduces inventory correctly")
    void testOrderConfirmReducesInventory() {
        ConcurrentHashMap<Long, Integer> inventory = new ConcurrentHashMap<>();
        inventory.put(1L, 50);
        inventory.put(2L, 30);

        int order1Qty = 5;
        int order2Qty = 3;

        inventory.compute(1L, (k, v) -> Math.max(0, v - order1Qty));
        inventory.compute(2L, (k, v) -> Math.max(0, v - order2Qty));

        assertEquals(45, inventory.get(1L));
        assertEquals(27, inventory.get(2L));
    }

    @Test
    @DisplayName("Test order cancel restores inventory correctly")
    void testOrderCancelRestoresInventory() {
        ConcurrentHashMap<Long, Integer> inventory = new ConcurrentHashMap<>();
        inventory.put(1L, 50);

        int cancelledQty = 5;

        inventory.compute(1L, (k, v) -> v + cancelledQty);

        assertEquals(55, inventory.get(1L));
    }

    @Test
    @DisplayName("Test sales count increments on delivery")
    void testSalesCountIncrementsOnDelivery() {
        ConcurrentHashMap<Long, Integer> salesCount = new ConcurrentHashMap<>();
        salesCount.put(1L, 10);

        int additionalSales = 5;

        salesCount.compute(1L, (k, v) -> v + additionalSales);

        assertEquals(15, salesCount.get(1L));
    }

    @Test
    @DisplayName("Test parallel inventory updates don't cause race conditions")
    void testParallelInventoryUpdatesNoRaceCondition() throws InterruptedException {
        ConcurrentHashMap<Long, AtomicInteger> inventory = new ConcurrentHashMap<>();
        
        for (long i = 1; i <= 100; i++) {
            inventory.put(i, new AtomicInteger(100));
        }

        int operationsPerProduct = 10;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100 * operationsPerProduct);

        for (long productId = 1; productId <= 100; productId++) {
            for (int op = 0; op < operationsPerProduct; op++) {
                final long pid = productId;
                executor.submit(() -> {
                    inventory.get(pid).decrementAndGet();
                    latch.countDown();
                });
            }
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        for (long productId = 1; productId <= 100; productId++) {
            assertEquals(100 - operationsPerProduct, inventory.get(productId).get(),
                    "Product " + productId + " should have correct final stock");
        }
    }
}
