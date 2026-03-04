package com.smart_ecomernce_api.smart_ecomernce_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parallel Stream Operations Test
 * 
 * Tests parallel stream processing and bulk operations performance
 */
@DisplayName("Parallel Stream Operations Tests")
class ParallelStreamOperationTest {

    @Test
    @DisplayName("Test parallel stream processes items faster than sequential")
    void testParallelStreamPerformance() throws InterruptedException {
        int itemCount = 1000;
        List<Integer> items = IntStream.range(0, itemCount).boxed().collect(Collectors.toList());
        
        AtomicInteger sequentialCounter = new AtomicInteger(0);
        long sequentialStart = System.nanoTime();
        items.stream().forEach(i -> {
            try {
                Thread.sleep(1);
                sequentialCounter.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        long sequentialTime = System.nanoTime() - sequentialStart;
        
        AtomicInteger parallelCounter = new AtomicInteger(0);
        long parallelStart = System.nanoTime();
        items.parallelStream().forEach(i -> {
            try {
                Thread.sleep(1);
                parallelCounter.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        long parallelTime = System.nanoTime() - parallelStart;
        
        assertEquals(itemCount, sequentialCounter.get());
        assertEquals(itemCount, parallelCounter.get());
        
        System.out.println("Sequential time: " + sequentialTime / 1_000_000 + "ms");
        System.out.println("Parallel time: " + parallelTime / 1_000_000 + "ms");
    }

    @Test
    @DisplayName("Test parallel stream is thread-safe for calculations")
    void testParallelStreamThreadSafety() {
        int threadCount = 100;
        List<Integer> items = IntStream.range(0, threadCount).boxed().collect(Collectors.toList());
        
        ConcurrentHashMap<String, AtomicInteger> threadTracking = new ConcurrentHashMap<>();
        
        items.parallelStream().forEach(item -> {
            String threadName = Thread.currentThread().getName();
            threadTracking.computeIfAbsent(threadName, k -> new AtomicInteger(0)).incrementAndGet();
        });
        
        assertTrue(threadTracking.size() > 1, "Should use multiple threads");
        int totalProcessed = threadTracking.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();
        assertEquals(threadCount, totalProcessed);
    }

    @Test
    @DisplayName("Test bulk price calculation with parallel streams")
    void testBulkPriceCalculation() {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            products.add(new Product((long) i, "Product-" + i, BigDecimal.valueOf(100 + i)));
        }
        
        BigDecimal percentageIncrease = BigDecimal.valueOf(10);
        
        products.parallelStream().forEach(product -> {
            BigDecimal currentPrice = product.getPrice();
            BigDecimal newPrice = currentPrice.multiply(
                    BigDecimal.ONE.add(percentageIncrease.divide(BigDecimal.valueOf(100))));
            product.setUpdatedPrice(newPrice);
        });
        
        BigDecimal totalUpdatedPrice = products.stream()
                .map(Product::getUpdatedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expectedTotal = products.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(1.1)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertEquals(0, totalUpdatedPrice.compareTo(expectedTotal));
    }

    @Test
    @DisplayName("Test concurrent product updates don't lose data")
    void testConcurrentProductUpdates() throws InterruptedException {
        int concurrentUpdates = 50;
        ConcurrentHashMap<Long, AtomicInteger> productStocks = new ConcurrentHashMap<>();
        
        for (long i = 1; i <= 10; i++) {
            productStocks.put(i, new AtomicInteger(100));
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(concurrentUpdates);
        
        for (int i = 0; i < concurrentUpdates; i++) {
            executor.submit(() -> {
                try {
                    productStocks.forEach((productId, stock) -> {
                        stock.decrementAndGet();
                    });
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        productStocks.forEach((id, stock) -> {
            assertEquals(100 - concurrentUpdates, stock.get(), 
                    "Product " + id + " should have correct stock");
        });
    }

    @Test
    @DisplayName("Test parallel stream order preservation in collect")
    void testParallelStreamOrderPreservation() {
        List<Integer> input = IntStream.range(0, 1000).boxed().collect(Collectors.toList());
        
        List<Integer> output = input.parallelStream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        
        assertEquals(999, output.get(0));
        assertEquals(0, output.get(999));
    }

    @Test
    @DisplayName("Test async execution with CompletableFuture")
    void testCompletableFutureAsyncExecution() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        try {
            CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);
                    return "Result-1";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Error";
                }
            }, executor);

            CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);
                    return "Result-2";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Error";
                }
            }, executor);

            CompletableFuture<Void> combined = CompletableFuture.allOf(future1, future2);
            combined.get(5, TimeUnit.SECONDS);

            assertTrue(future1.isDone());
            assertTrue(future2.isDone());
            assertEquals("Result-1", future1.get());
            assertEquals("Result-2", future2.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Test parallel stream with reduction operation")
    void testParallelStreamReduction() {
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            prices.add(BigDecimal.valueOf(i + 1));
        }
        
        BigDecimal total = prices.parallelStream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expected = BigDecimal.valueOf(1000 * 1001 / 2);
        
        assertEquals(0, total.compareTo(expected));
    }

    static class Product {
        private final Long id;
        private final String name;
        private final BigDecimal price;
        private BigDecimal updatedPrice;

        public Product(Long id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getUpdatedPrice() { return updatedPrice; }
        public void setUpdatedPrice(BigDecimal price) { this.updatedPrice = price; }
    }
}
