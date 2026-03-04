package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductStatisticsResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Enhanced Product Service Interface with Predicate Support
 */
public interface ProductService {

    // ==================== CRUD Operations ====================

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySlug(String slug);

    ProductResponse getProductBySku(String sku);

    void deleteProduct(Long id);

    // ==================== Predicate-based Queries ====================

    /**
     * Find products using QueryDSL Predicate
     */
    Page<ProductResponse> findByPredicate(Predicate predicate, Pageable pageable);



    Page<ProductResponse> getAllProducts(Pageable pageable);

    // ==================== Specialized Queries ====================

    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);

    Page<ProductResponse> getProductsByCategoryName(String categoryName, Pageable pageable);

    Page<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<ProductResponse> getDiscountedProducts(Pageable pageable);

    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);

    Page<ProductResponse> getFeaturedProducts(Pageable pageable);

    Page<ProductResponse> getNewProducts(Pageable pageable);

    Page<ProductResponse> getBestsellerProducts(Pageable pageable);

    Page<ProductResponse> getTopRatedProducts(Pageable pageable);

    List<ProductResponse> getTrendingProducts(Long categoryId, int limit);

    Page<ProductResponse> findByInventoryStatus(InventoryStatus status, Pageable pageable);

    Page<ProductResponse> getProductsNeedingReorder(Pageable pageable);

    // ==================== Stock Management ====================

    ProductResponse reduceStock(Long productId, Integer quantity);

    void restoreStock(Long productId, Integer quantity);

    void reserveStock(Long productId, Integer quantity);

    void releaseReservedStock(Long productId, Integer quantity);

    // ==================== Bulk Operations ====================

    void bulkUpdateFeatured(List<Long> productIds, Boolean featured);

    void bulkDelete(List<Long> productIds);

    void bulkUpdatePrices(List<Long> productIds, BigDecimal priceChange, boolean isPercentage);

    // ==================== Statistics ====================

    ProductStatisticsResponse getProductStatistics();

    // ==================== Async Operations ====================

    void recalculateAllProductStatsAsync();

    void updateProductSalesCountAsync(Long productId, Integer quantity);

    // ==================== Helper Methods ====================

    void incrementViewCount(Long productId);
}
