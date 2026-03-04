package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Category entity with advanced filtering
 */
@Repository
public interface CategoryRepository extends BaseRepository<Category, Long> {

    // Basic queries
    Optional<Category> findBySlugAndIsActiveTrue(String slug);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    // Find all active categories
    List<Category> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    Page<Category> findByIsActiveTrue(Pageable pageable);

    // Featured categories
    List<Category> findByFeaturedTrueAndIsActiveTrueOrderByDisplayOrderAscNameAsc();

    Page<Category> findByFeaturedTrueAndIsActiveTrue(Pageable pageable);

    // Search by name or description
    @Query("SELECT c FROM Category c WHERE " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Category> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.isActive = true AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Category> searchActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Advanced filtering
    @Query("SELECT c FROM Category c WHERE " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:isActive IS NULL OR c.isActive = :isActive) AND " +
            "(:featured IS NULL OR c.featured = :featured) AND " +
            "(:hasProducts IS NULL OR " +
            "   (:hasProducts = true AND EXISTS (SELECT p FROM Product p WHERE p.category.id = c.id AND p.isActive = true)) OR " +
            "   (:hasProducts = false AND NOT EXISTS (SELECT p FROM Product p WHERE p.category.id = c.id AND p.isActive = true)))")
    Page<Category> findByFilters(
            @Param("name") String name,
            @Param("isActive") Boolean isActive,
            @Param("featured") Boolean featured,
            @Param("hasProducts") Boolean hasProducts,
            Pageable pageable);

    // Categories with products
    @Query("SELECT DISTINCT c FROM Category c WHERE c.isActive = true AND " +
            "EXISTS (SELECT p FROM Product p WHERE p.category.id = c.id AND p.isActive = true)")
    List<Category> findCategoriesWithProducts();

    @Query("SELECT DISTINCT c FROM Category c WHERE c.isActive = true AND " +
            "EXISTS (SELECT p FROM Product p WHERE p.category.id = c.id AND p.isActive = true)")
    Page<Category> findCategoriesWithProducts(Pageable pageable);

    // Count active products in category
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    long countActiveProductsInCategory(@Param("categoryId") Long categoryId);

    // Categories sorted by product count
    @Query("SELECT c FROM Category c " +
            "LEFT JOIN c.products p " +
            "WHERE c.isActive = true AND (p.isActive = true OR p IS NULL) " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(p) DESC")
    Page<Category> findCategoriesByProductCount(Pageable pageable);

    // Categories created in date range
    @Query("SELECT c FROM Category c WHERE " +
            "c.createdAt BETWEEN :startDate AND :endDate")
    Page<Category> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Categories updated in date range
    @Query("SELECT c FROM Category c WHERE " +
            "c.updatedAt BETWEEN :startDate AND :endDate")
    Page<Category> findByUpdatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Find by multiple slugs
    List<Category> findBySlugIn(List<String> slugs);

    // Find by multiple IDs
    List<Category> findByIdIn(List<Long> ids);

    // Dynamic filtering using Specification
    @Query("SELECT c FROM Category c WHERE " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:description IS NULL OR LOWER(c.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
            "(:isActive IS NULL OR c.isActive = :isActive) AND " +
            "(:featured IS NULL OR c.featured = :featured) AND " +
            "(:minOrder IS NULL OR c.displayOrder >= :minOrder) AND " +
            "(:maxOrder IS NULL OR c.displayOrder <= :maxOrder)")
    Page<Category> findByAdvancedFilters(
            @Param("name") String name,
            @Param("description") String description,
            @Param("isActive") Boolean isActive,
            @Param("featured") Boolean featured,
            @Param("minOrder") Integer minOrder,
            @Param("maxOrder") Integer maxOrder,
            Pageable pageable);
}