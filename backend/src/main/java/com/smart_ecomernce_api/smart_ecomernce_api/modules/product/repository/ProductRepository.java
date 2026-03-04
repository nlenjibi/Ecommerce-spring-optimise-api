package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Product entity with QueryDSL Predicate support
 * 
 * Optimization notes:
 * - Uses @EntityGraph to eagerly load relationships and avoid N+1 queries
 * - JOIN FETCH queries for additionalImages and tags
 * - Indexed columns defined in Product entity for query performance
 */
@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

        /**
         * Find product by slug with all relationships eagerly loaded
         */
        @EntityGraph(attributePaths = { "category", "images", "additionalImages", "tags" })
        @Query("SELECT p FROM Product p WHERE p.slug = :slug AND p.isActive = true")
        Optional<Product> findBySlugAndIsActiveTrue(@Param("slug") String slug);

        /**
         * Optimized product listing with JOIN FETCH for N+1 prevention
         * Loads all necessary relationships in single query
         */
        @EntityGraph(attributePaths = {"additionalImages", "tags"})
        @Query("SELECT p FROM Product p WHERE p.isActive = :isActive")
        Page<Product> findAllOptimized(@Param("isActive") boolean isActive, Pageable pageable);

        /**
         * Optimized trending products query with full relationship loading
         */
        @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.additionalImages LEFT JOIN FETCH p.tags WHERE p.isActive = true ORDER BY p.salesCount DESC, p.ratingAverage DESC",
               countQuery = "SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
        Page<Product> findTrendingProductsOptimized(Pageable pageable);

        /**
         * Optimized discounted products query
         */
        @Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.additionalImages LEFT JOIN FETCH p.tags WHERE p.isActive = true AND p.discountPrice IS NOT NULL AND p.discountPrice > 0 AND p.discountPrice < p.price",
               countQuery = "SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.discountPrice IS NOT NULL AND p.discountPrice > 0")
        Page<Product> findDiscountedProductsOptimized(Pageable pageable);

        /**
         * Find product by SKU with full relationships
         */
        @EntityGraph(attributePaths = { "category", "images", "additionalImages", "tags" })
        @Query("SELECT p FROM Product p WHERE p.sku = :sku AND p.isActive = true")
        Optional<Product> findBySkuAndIsActiveTrue(@Param("sku") String sku);

        /**
         * Check if slug exists
         */
        @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.slug = :slug AND p.isActive = true")
        boolean existsBySlugAndIsActiveTrue(@Param("slug") String slug);

        /**
         * Check if SKU exists
         */
        @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.sku = :sku AND p.isActive = true")
        boolean existsBySkuAndIsActiveTrue(@Param("sku") String sku);

        /**
         * Find products by category ID
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
        Page<Product> findByCategoryIdAndIsActiveTrue(@Param("categoryId") Long categoryId, Pageable pageable);

        /**
         * Find products by category name
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.category.name = :categoryName AND p.isActive = true")
        Page<Product> findByCategoryNameAndIsActiveTrue(@Param("categoryName") String categoryName,
                        Pageable pageable);

        /**
         * Find products by inventory status
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.inventoryStatus = :status AND p.isActive = true")
        Page<Product> findByInventoryStatusAndIsActiveTrue(@Param("status") InventoryStatus status, Pageable pageable);

        /**
         * Find featured products
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.featured = true AND p.isActive = true")
        Page<Product> findByFeaturedTrueAndIsActiveTrue(Pageable pageable);

        /**
         * Find new products
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isNew = true AND p.isActive = true")
        Page<Product> findByIsNewTrueAndIsActiveTrue(Pageable pageable);

        /**
         * Find bestseller products
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isBestseller = true AND p.isActive = true")
        Page<Product> findByIsBestsellerTrueAndIsActiveTrue(Pageable pageable);

        /**
         * Find products by price range (considers discount price)
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
                        "COALESCE(p.discountPrice, p.price) BETWEEN :minPrice AND :maxPrice")
        Page<Product> findByPriceRangeAndIsActiveTrue(@Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        Pageable pageable);

        /**
         * Alias for findByPriceRangeAndIsActiveTrue for service compatibility
         */
        default Page<Product> findByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable) {
                return findByPriceRangeAndIsActiveTrue(min, max, pageable);
        }

        /**
         * Alias for findByCategoryNameAndIsActiveTrue for service
         * compatibility
         */
        default Page<Product> findByCategoryName(String categoryName, Pageable pageable) {
                return findByCategoryNameAndIsActiveTrue(categoryName, pageable);
        }

        /**
         * Find discounted products
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
                        "p.discountPrice IS NOT NULL AND p.discountPrice > 0 AND p.discountPrice < p.price")
        Page<Product> findDiscountedProductsAndIsActiveTrue(Pageable pageable);

        /**
         * Search products by keyword (name, description, SKU)
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
                        "(p.name LIKE CONCAT('%', :keyword, '%') OR " +
                        "p.sku LIKE CONCAT('%', :keyword, '%'))")
        Page<Product> searchProductsAndIsActiveTrue(@Param("keyword") String keyword, Pageable pageable);

        /**
         * Find products created between dates
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.createdAt BETWEEN :startDate AND :endDate")
        Page<Product> findByCreatedAtBetweenAndIsActiveTrue(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Find recently created products
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.createdAt >= :sinceDate ORDER BY p.createdAt DESC")
        List<Product> findRecentProductsAndIsActiveTrue(@Param("sinceDate") LocalDateTime sinceDate);

        /**
         * Find recently created products with pagination
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.createdAt >= :sinceDate ORDER BY p.createdAt DESC")
        Page<Product> findRecentProductsAndIsActiveTrue(@Param("sinceDate") LocalDateTime sinceDate, Pageable pageable);

        /**
         * Find top-rated products
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.ratingAverage >= :minRating " +
                        "ORDER BY p.ratingAverage DESC")
        Page<Product> findTopRatedProductsAndIsActiveTrue(@Param("minRating") BigDecimal minRating, Pageable pageable);

        /**
         * Find products needing reorder
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.trackInventory = true AND " +
                        "p.stockQuantity <= p.reorderPoint")
        Page<Product> findProductsNeedingReorderAndIsActiveTrue(Pageable pageable);

        /**
         * Find low stock products
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.trackInventory = true AND " +
                        "p.inventoryStatus = 'LOW_STOCK'")
        Page<Product> findLowStockProductsAndIsActiveTrue(Pageable pageable);

        /**
         * Find out of stock products
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.trackInventory = true AND " +
                        "p.inventoryStatus = 'OUT_OF_STOCK'")
        Page<Product> findOutOfStockProductsAndIsActiveTrue(Pageable pageable);

        /**
         * Count products by category
         */
        @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
        long countByCategoryIdAndIsActiveTrue(@Param("categoryId") Long categoryId);

        /**
         * Count featured products
         */
        @Query("SELECT COUNT(p) FROM Product p WHERE p.featured = true AND p.isActive = true")
        long countByFeaturedTrueAndIsActiveTrue();

        /**
         * Count products by inventory status
         */
        @Query("SELECT COUNT(p) FROM Product p WHERE p.inventoryStatus = :status AND p.isActive = true")
        long countByInventoryStatusAndIsActiveTrue(@Param("status") InventoryStatus status);

        /**
         * Calculate average price
         */
        @Query("SELECT AVG(COALESCE(p.discountPrice, p.price)) FROM Product p WHERE p.isActive = true")
        Optional<BigDecimal> findAveragePriceAndIsActiveTrue();

        /**
         * Find price range (min and max)
         */
        @Query("SELECT MIN(COALESCE(p.discountPrice, p.price)), MAX(COALESCE(p.discountPrice, p.price)) " +
                        "FROM Product p WHERE p.isActive = true")
        List<Object[]> findPriceRangeAndIsActiveTrue();

        /**
         * Get inventory statistics
         */
        @Query("SELECT SUM(p.stockQuantity), SUM(p.reservedQuantity), " +
                        "SUM(p.stockQuantity - p.reservedQuantity) " +
                        "FROM Product p WHERE p.isActive = true AND p.trackInventory = true")
        List<Object[]> getInventoryStatisticsAndIsActiveTrue();

        /**
         * Get product count by category
         */
        @Query("SELECT c.id, c.name, COUNT(p) FROM Product p " +
                        "JOIN p.category c WHERE p.isActive = true " +
                        "GROUP BY c.id, c.name ORDER BY COUNT(p) DESC")
        List<Object[]> getProductCountByCategoryAndIsActiveTrue();

        /**
         * Get detailed inventory statistics
         */
        @Query("SELECT p.inventoryStatus, COUNT(p), SUM(p.stockQuantity), " +
                        "SUM(p.reservedQuantity) FROM Product p " +
                        "WHERE p.isActive = true GROUP BY p.inventoryStatus")
        List<Object[]> getDetailedInventoryStatisticsAndIsActiveTrue();

        /**
         * Increment view count
         */
        @Modifying
        @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1, " +
                        "p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId")
        int incrementViewCountAndIsActiveTrue(@Param("productId") Long productId);

        /**
         * Increment sales count
         */
        @Modifying
        @Query("UPDATE Product p SET p.salesCount = p.salesCount + :quantity, " +
                        "p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId")
        int incrementSalesCountAndIsActiveTrue(@Param("productId") Long productId, @Param("quantity") Integer quantity);

        /**
         * Update stock quantity
         */
        @Modifying
        @Query("UPDATE Product p SET p.stockQuantity = :quantity, " +
                        "p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId AND p.isActive = true")
        int updateStockAndIsActiveTrue(@Param("productId") Long productId, @Param("quantity") Integer quantity);

        /**
         * Reserve stock
         */
        @Modifying
        @Query("UPDATE Product p SET p.reservedQuantity = p.reservedQuantity + :quantity, " +
                        "p.updatedAt = CURRENT_TIMESTAMP " +
                        "WHERE p.id = :productId AND (p.stockQuantity - p.reservedQuantity) >= :quantity AND p.isActive = true")
        int reserveStockAndIsActiveTrue(@Param("productId") Long productId, @Param("quantity") Integer quantity);

        /**
         * Release reserved stock
         */
        @Modifying
        @Query("UPDATE Product p SET p.reservedQuantity = GREATEST(0, p.reservedQuantity - :quantity), " +
                        "p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :productId AND p.isActive = true")
        int releaseReservedStockAndIsActiveTrue(@Param("productId") Long productId,
                        @Param("quantity") Integer quantity);

        /**
         * Deduct stock
         */
        @Modifying
        @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity, " +
                        "p.reservedQuantity = GREATEST(0, p.reservedQuantity - :quantity), " +
                        "p.updatedAt = CURRENT_TIMESTAMP " +
                        "WHERE p.id = :productId AND p.stockQuantity >= :quantity AND p.isActive = true")
        int deductStockAndIsActiveTrue(@Param("productId") Long productId, @Param("quantity") Integer quantity);

        /**
         * Bulk update featured status
         */
        @Modifying
        @Query("UPDATE Product p SET p.featured = :featured, " +
                        "p.updatedAt = CURRENT_TIMESTAMP WHERE p.id IN :productIds AND p.isActive = true")
        int bulkUpdateFeaturedAndIsActiveTrue(@Param("productIds") List<Long> productIds,
                        @Param("featured") Boolean featured);

        /**
         * Bulk soft delete
         */
        @Modifying
        @Query("UPDATE Product p SET p.isActive = false, " +
                        "p.updatedAt = CURRENT_TIMESTAMP WHERE p.id IN :productIds")
        int bulkSoftDelete(@Param("productIds") List<Long> productIds);

        /**
         * Check stock availability
         */
        @Query("SELECT CASE WHEN (p.stockQuantity - p.reservedQuantity) >= :quantity " +
                        "THEN true ELSE false END " +
                        "FROM Product p WHERE p.id = :productId AND p.isActive = true")
        boolean hasSufficientStockAndIsActiveTrue(@Param("productId") Long productId,
                        @Param("quantity") Integer quantity);

        /**
         * Find products by multiple category IDs
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds AND p.isActive = true")
        Page<Product> findByCategoryIdInAndIsActiveTrue(@Param("categoryIds") List<Long> categoryIds,
                        Pageable pageable);

        /**
         * Find trending products (high sales and ratings)
         */
        @EntityGraph(attributePaths = { "category", "images" })
        @Query("SELECT p FROM Product p WHERE p.isActive = true " +
                        "ORDER BY p.salesCount DESC, p.ratingAverage DESC, p.viewCount DESC")
        Page<Product> findTrendingProductsAndIsActiveTrue(Pageable pageable);

        /**
         * Find products with lock for update
         */
        @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isActive = true")
        Optional<Product> findByIdWithLockAndIsActiveTrue(@Param("id") Long id);

        /**
         * Find all products matching a QueryDSL Predicate (for advanced filtering)
         */
        @EntityGraph(attributePaths = {"category", "images"})
        Page<Product> findAll(Predicate predicate, Pageable pageable);


        /**
         * Find product by ID with category and images eagerly loaded (solves N+1)
         */
        @EntityGraph(attributePaths = {"category", "images"})
        @Query("SELECT p FROM Product p WHERE p.id = :id AND p.isActive = true")
        Optional<Product> findByIdWithCategoryAndImagesAndIsActiveTrue(@Param("id") Long id);

        /**
         * Find multiple products by their IDs - used for bulk operations with parallel processing
         */
        @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.isActive = true")
        List<Product> findByIdIn(@Param("ids") List<Long> ids);
}
