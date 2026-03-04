/**
 * ProductServiceImpl with consistent caching.
 *
 * Caches used:
 * - products
 * - products-page
 * - products-search
 * - products-category
 * - products-category-name
 * - products-price-range
 * - products-discounted
 * - products-featured
 * - products-new
 * - products-bestseller
 * - products-top-rated
 * - products-trending
 * - products-status
 * - products-reorder
 * - products-predicate
 * - products-filter
 *
 * Ensure your cache manager is configured for all these cache names.
 */

package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.impl;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.DuplicateResourceException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.InvalidDataException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.ProductPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.mapper.ProductMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.ProductService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

        private final ProductMapper productMapper;
        private final ProductRepository productRepository;
        private final CategoryRepository categoryRepository;

        // ==================== CRUD Operations ====================

        @Override
        @Transactional
        @CachePut(value = "products", key = "#result.id")
        @CacheEvict(value = {
                "products-page", "products-search", "products-predicate", "products-filter",
                "products-category", "products-category-name", "products-status",
                "products-featured", "products-discounted", "products-new", "products-bestseller"
        }, allEntries = true)
        public ProductResponse createProduct(ProductCreateRequest request) {
                // Validate category exists
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Category not found with ID: " + request.getCategoryId()));

                // Auto-generate slug if missing or blank
                String slug = request.getSlug();
                if (slug == null || slug.isBlank()) {
                        slug = request.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
                }
                // Check slug uniqueness
                if (productRepository.existsBySlugAndIsActiveTrue(slug)) {
                        throw new DuplicateResourceException("Product with slug already exists: " + slug);
                }

                // Check SKU uniqueness
                if (request.getSku() != null &&
                        productRepository.existsBySkuAndIsActiveTrue(request.getSku())) {
                        throw new DuplicateResourceException("Product with SKU already exists: " + request.getSku());
                }

                Product product = productMapper.toEntity(request);
                product.setSlug(slug);
                product.setCategory(category);
                product.updateInventoryStatus();

                Product savedProduct = productRepository.save(product);
                log.info("Product created with id: {}", savedProduct.getId());

                return productMapper.toDto(savedProduct);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products", key = "#id")
        public ProductResponse getProductById(Long id) {
                Product product = productRepository.findByIdWithCategoryAndImagesAndIsActiveTrue(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

                // Skip view count increment for now to avoid transaction issues
                // incrementViewCountAsync(id);

                return productMapper.toDto(product);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products", key = "'slug:' + #slug")
        public ProductResponse getProductBySlug(String slug) {
                Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found with slug: " + slug));

                // Skip view count increment for now to avoid transaction issues
                // incrementViewCountAsync(product.getId());

                return productMapper.toDto(product);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products", key = "'sku:' + #sku")
        public ProductResponse getProductBySku(String sku) {
                Product product = productRepository.findBySkuAndIsActiveTrue(sku)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));

                // Skip view count increment for now to avoid transaction issues
                // incrementViewCountAsync(product.getId());

                return productMapper.toDto(product);
        }

        @Override
        @Transactional
        @CachePut(value = "products", key = "#id")
        @CacheEvict(value = {
                "products-page", "products-search", "products-predicate", "products-filter",
                "products-category", "products-category-name", "products-status",
                "products-featured", "products-discounted", "products-new", "products-bestseller"
        }, allEntries = true)
        public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
                Product product = productRepository.findByIdAndIsActiveTrue(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

                // Validate SKU uniqueness when changed.
                if (request.getSku() != null
                        && !request.getSku().equals(product.getSku())
                        && productRepository.existsBySkuAndIsActiveTrue(request.getSku())) {
                        throw new DuplicateResourceException("Product with SKU already exists: " + request.getSku());
                }

                // Validate discounted price against effective target price.
                if (request.getDiscountedPrice() != null) {
                        if (request.getDiscountedPrice().compareTo(BigDecimal.ZERO) <= 0) {
                                throw new InvalidDataException("Discounted price must be greater than 0");
                        }
                        BigDecimal basePrice = request.getPrice() != null ? request.getPrice() : product.getPrice();
                        if (basePrice != null && request.getDiscountedPrice().compareTo(basePrice) >= 0) {
                                throw new InvalidDataException("Discounted price must be less than regular price");
                        }
                }

                // Update category if provided
                if (request.getCategoryId() != null &&
                        !request.getCategoryId().equals(product.getCategory().getId())) {
                        Category category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Category not found with ID: " + request.getCategoryId()));
                        product.setCategory(category);
                }

                productMapper.update(request, product);
                product.updateInventoryStatus();

                Product updatedProduct = productRepository.save(product);
                log.info("Product updated with id: {}", id);

                return productMapper.toDto(updatedProduct);
        }

        @Override
        @Transactional
        @CacheEvict(value = {
                "products", "products-page", "products-search", "products-predicate",
                "products-filter", "products-category", "products-category-name",
                "products-status", "products-featured", "products-discounted",
                "products-new", "products-bestseller"
        }, allEntries = true)
        public void deleteProduct(Long id) {
                Product product = productRepository.findByIdAndIsActiveTrue(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

                product.setIsActive(false);
                productRepository.save(product);
                log.info("Product soft deleted with id: {}", id);
        }

        // ==================== Predicate-based Queries ====================

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-predicate", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#predicate=' + #predicate.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> findByPredicate(Predicate predicate, Pageable pageable) {
                return productRepository.findAll(predicate, pageable)
                        .map(productMapper::toDto);
        }

        @Transactional(readOnly = true)
        @Cacheable(value = "products-filter", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#filter=' + #filter.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> findByFilters(ProductFilterRequest filter, Pageable pageable) {
                filter.validate();

                // Convert filter to predicate using our ProductPredicates utility
                Predicate predicate = ProductPredicates
                        .fromFilterRequest(filter);

                return productRepository.findAll(predicate, pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-page", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getAllProducts(Pageable pageable) {
                return productRepository.findAllOptimized(true, pageable)
                        .map(productMapper::toDto);
        }

        // ==================== Specialized Queries ====================

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-category", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#categoryId=' + #categoryId + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
                return productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-category-name", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#categoryName=' + #categoryName + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getProductsByCategoryName(String categoryName, Pageable pageable) {
                return productRepository.findByCategoryName(categoryName, pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-price-range", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#minPrice=' + #minPrice + '&maxPrice=' + #maxPrice + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice,
                                                             Pageable pageable) {
                return productRepository.findByPriceRange(minPrice, maxPrice, pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-discounted", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getDiscountedProducts(Pageable pageable) {
                return productRepository.findDiscountedProductsOptimized(pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-search", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#keyword=' + #keyword + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
                if (keyword == null || keyword.isBlank()) {
                        return getAllProducts(pageable);
                }
                return productRepository.searchProductsAndIsActiveTrue(keyword, pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-featured", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getFeaturedProducts(Pageable pageable) {
                return productRepository.findByFeaturedTrueAndIsActiveTrue(pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-new", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getNewProducts(Pageable pageable) {
                return productRepository.findByIsNewTrueAndIsActiveTrue(pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-bestseller", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getBestsellerProducts(Pageable pageable) {
                return productRepository.findByIsBestsellerTrueAndIsActiveTrue(pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-top-rated", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getTopRatedProducts(Pageable pageable) {
                return productRepository.findTopRatedProductsAndIsActiveTrue(BigDecimal.valueOf(4.0), pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-trending", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#categoryId=' + #categoryId + '&limit=' + #limit).getBytes())")
        public List<ProductResponse> getTrendingProducts(Long categoryId, int limit) {
                Pageable pageable = PageRequest.of(0, limit);
                Page<Product> productPage = productRepository.findTrendingProductsOptimized(pageable);
                return productPage.getContent().stream()
                        .map(productMapper::toDto)
                        .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-status", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#status=' + #status + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> findByInventoryStatus(InventoryStatus status, Pageable pageable) {
                return productRepository.findByInventoryStatusAndIsActiveTrue(status, pageable)
                        .map(productMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "products-reorder", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
        public Page<ProductResponse> getProductsNeedingReorder(Pageable pageable) {
                return productRepository.findProductsNeedingReorderAndIsActiveTrue(pageable)
                        .map(productMapper::toDto);
        }

        // ==================== Stock Management ====================

        @Override
        @Transactional
        @CacheEvict(value = {
                "products", "products-status", "products-reorder", "products-trending"
        }, allEntries = true)
        public ProductResponse reduceStock(Long productId, Integer quantity) {
                Product product = productRepository.findByIdWithLockAndIsActiveTrue(productId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found with ID: " + productId));

                product.deductStock(quantity);
                Product updatedProduct = productRepository.save(product);

                log.info("Stock reduced for product {} by {}", productId, quantity);
                return productMapper.toDto(updatedProduct);
        }

        @Override
        @Transactional
        @CacheEvict(value = {
                "products", "products-status", "products-reorder", "products-trending"
        }, allEntries = true)
        public void restoreStock(Long productId, Integer quantity) {
                Product product = productRepository.findByIdWithLockAndIsActiveTrue(productId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found with ID: " + productId));

                product.addStock(quantity);
                productRepository.save(product);

                log.info("Stock restored for product {} by {}", productId, quantity);
        }

        @Override
        @Transactional
        @CacheEvict(value = {
                "products", "products-status", "products-reorder", "products-trending"
        }, allEntries = true)
        public void reserveStock(Long productId, Integer quantity) {
                Product product = productRepository.findByIdWithLockAndIsActiveTrue(productId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found with ID: " + productId));

                product.reserveStock(quantity);
                productRepository.save(product);

                log.info("Stock reserved for product {} by {}", productId, quantity);
        }

        @Override
        @Transactional
        @CacheEvict(value = {
                "products", "products-status", "products-reorder", "products-trending"
        }, allEntries = true)
        public void releaseReservedStock(Long productId, Integer quantity) {
                Product product = productRepository.findByIdWithLockAndIsActiveTrue(productId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found with ID: " + productId));

                product.releaseReservedStock(quantity);
                productRepository.save(product);

                log.info("Reserved stock released for product {} by {}", productId, quantity);
        }

        // ==================== Bulk Operations ====================

        @Override
        @Transactional
        @CacheEvict(value = {
                "products", "products-status", "products-reorder", "products-trending"
        }, allEntries = true)
        public void bulkUpdateFeatured(List<Long> productIds, Boolean featured) {
                int updatedCount = productRepository.bulkUpdateFeaturedAndIsActiveTrue(productIds, featured);
                log.info("Bulk updated featured status for {} products to {}", updatedCount, featured);
        }

        @Override
        @Transactional
        @CacheEvict(value = {
                "products", "products-search", "products-predicate", "products-filter",
                "products-category", "products-category-name", "products-page"
        }, allEntries = true)
        public void bulkDelete(List<Long> productIds) {
                int deletedCount = productRepository.bulkSoftDelete(productIds);
                log.info("Bulk soft deleted {} products", deletedCount);
        }

        @Override
        @Transactional
        @CacheEvict(value = {
                "products", "products-page", "products-search", "products-predicate",
                "products-filter", "products-discounted", "products-featured"
        }, allEntries = true)
        public void bulkUpdatePrices(List<Long> productIds, BigDecimal priceChange, boolean isPercentage) {
                List<Product> products = productRepository.findByIdIn(productIds);
                
                products.parallelStream().forEach(product -> {
                        BigDecimal currentPrice = product.getPrice();
                        BigDecimal newPrice;
                        
                        if (isPercentage) {
                                newPrice = currentPrice.multiply(BigDecimal.ONE.add(priceChange.divide(BigDecimal.valueOf(100))));
                        } else {
                                newPrice = currentPrice.add(priceChange);
                        }
                        
                        if (newPrice.compareTo(BigDecimal.ZERO) > 0) {
                                product.setPrice(newPrice);
                        }
                });
                
                productRepository.saveAll(products);
                log.info("Bulk updated prices for {} products using {} parallel processing", 
                        products.size(), isPercentage ? "percentage" : "fixed amount");
        }

        // ==================== Statistics ====================

        @Override
        @Transactional(readOnly = true)
        public ProductStatisticsResponse getProductStatistics() {
                long totalProducts = productRepository.countByIsActiveTrue();
                BigDecimal averagePrice = productRepository.findAveragePriceAndIsActiveTrue().orElse(BigDecimal.ZERO);

                List<Object[]> categoryStats = productRepository.getProductCountByCategoryAndIsActiveTrue();
                Map<String, Long> productsByCategory = categoryStats.stream()
                        .collect(Collectors.toMap(
                                obj -> (String) obj[1], // category name
                                obj -> (Long) obj[2] // count
                        ));

                List<Object[]> inventoryStats = productRepository.getDetailedInventoryStatisticsAndIsActiveTrue();
                Map<String, Long> productsByStatus = inventoryStats.stream()
                        .collect(Collectors.toMap(
                                obj -> ((InventoryStatus) obj[0]).name(),
                                obj -> (Long) obj[1]));

                List<Object[]> inventoryValueStats = productRepository.getInventoryStatisticsAndIsActiveTrue();
                BigDecimal totalInventoryValue = BigDecimal.ZERO;
                if (inventoryValueStats != null && !inventoryValueStats.isEmpty()) {
                        Object[] stats = inventoryValueStats.get(0);
                        if (stats != null && stats[0] != null) {
                                totalInventoryValue = new BigDecimal(stats[0].toString());
                        }
                }

                return ProductStatisticsResponse.builder()
                        .totalProducts(totalProducts)
                        .activeProducts(totalProducts) // Since we only count active products
                        .featuredProducts(productRepository.countByFeaturedTrueAndIsActiveTrue())
                        .outOfStockProducts(
                                productsByStatus.getOrDefault(InventoryStatus.OUT_OF_STOCK.name(), 0L))
                        .lowStockProducts(productsByStatus.getOrDefault(InventoryStatus.LOW_STOCK.name(), 0L))
                        .averagePrice(averagePrice)
                        .totalInventoryValue(totalInventoryValue)
                        .productsByCategory(productsByCategory)
                        .productsByStatus(productsByStatus)
                        .build();
        }

        // ==================== Helper Methods ====================

        @Override
        @Transactional
        public void incrementViewCount(Long productId) {
                try {
                        productRepository.incrementViewCountAndIsActiveTrue(productId);
                } catch (Exception e) {
                        log.warn("Failed to increment view count for product {}: {}", productId, e.getMessage());
                }
        }

        // ==================== Validation Methods ====================

        @Transactional(readOnly = true)
        public boolean existsBySlug(String slug) {
                return productRepository.existsBySlugAndIsActiveTrue(slug);
        }

        @Transactional(readOnly = true)
        public boolean existsBySku(String sku) {
                return productRepository.existsBySkuAndIsActiveTrue(sku);
        }

        @Transactional(readOnly = true)
        public boolean hasSufficientStock(Long productId, Integer quantity) {
                return productRepository.hasSufficientStockAndIsActiveTrue(productId, quantity);
        }

        // ==================== Async Operations ====================

        @Override
        @Async("taskExecutor")
        @Transactional
        public void recalculateAllProductStatsAsync() {
                try {
                        List<Product> products = productRepository.findAll();
                        
                        products.parallelStream().forEach(product -> {
                                product.updateInventoryStatus();
                        });
                        
                        productRepository.saveAll(products);
                        log.info("Recalculated inventory stats for {} products asynchronously", products.size());
                } catch (Exception e) {
                        log.error("Failed to recalculate product stats: {}", e.getMessage());
                }
        }

        @Override
        @Async("taskExecutor")
        @Transactional
        public void updateProductSalesCountAsync(Long productId, Integer quantity) {
                try {
                        Product product = productRepository.findById(productId).orElse(null);
                        if (product != null) {
                                Long currentSales = product.getSalesCount() != null ? product.getSalesCount() : 0L;
                                product.setSalesCount(currentSales + quantity);
                                productRepository.save(product);
                                log.debug("Updated sales count for product {} by {}", productId, quantity);
                        }
                } catch (Exception e) {
                        log.error("Failed to update sales count for product {}: {}", productId, e.getMessage());
                }
        }


}