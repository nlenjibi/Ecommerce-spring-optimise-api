package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Advanced Product Filter Request DTO
 * Supports complex filtering with multiple criteria and predicate support
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Advanced product filtering criteria")
public class ProductFilterRequest {

    @Schema(description = "Filter by single category ID")
    private Long categoryId;

    @Schema(description = "Filter by description")
    private String description;

    @Schema(description = "Filter by multiple category IDs")
    private List<Long> categoryIds;

    @Schema(description = "Filter by category name (exact match, case-insensitive)")
    private String categoryName;

    @Schema(description = "Filter by category slug")
    private String categorySlug;

    @Schema(description = "Minimum price (considers discount price if available)")
    private BigDecimal minPrice;

    @Schema(description = "Minimum effective price (considers discount price if available)")
    private BigDecimal minEffectivePrice;

    @Schema(description = "Maximum price (considers discount price if available)")
    private BigDecimal maxPrice;

    @Schema(description = "Maximum effective price ( considers discount price if available)")
    private BigDecimal maxEffectivePrice;

    @Schema(description = "Search keyword (searches name, description, SKU)")
    private String keyword;

    @Schema(description = "Product name search")
    private String name;

    @Schema(description = "Product SKU")
    private String sku;

    @Schema(description = "Product slug")
    private String slug;

    @Schema(description = "Show only featured products")
    private Boolean featured;

    @Schema(description = "Show only new products")
    private Boolean isNew;

    @Schema(description = "Show only active products")
    private Boolean isActive;

    @Schema(description = "Show only bestseller products")
    private Boolean isBestseller;

    @Schema(description = "Filter by inventory status")
    private InventoryStatus inventoryStatus;

    @Schema(description = "Filter by multiple inventory statuses")
    private List<InventoryStatus> inventoryStatuses;

    @Schema(description = "Show only in-stock products")
    private Boolean inStockOnly;

    @Schema(description = "Show only low stock products")
    private Boolean lowStockOnly;

    @Schema(description = "Show only out of stock products")
    private Boolean outOfStockOnly;

    @Schema(description = "Show only products needing reorder")
    private Boolean needsReorderOnly;

    @Schema(description = "Show only discounted products")
    private Boolean hasDiscount;

    @Schema(description = "Minimum discount percentage")
    private Integer minDiscountPercent;

    @Schema(description = "Maximum discount percentage")
    private Integer maxDiscountPercent;

    @Schema(description = "Minimum rating")
    private BigDecimal minRating;

    @Schema(description = "Maximum rating")
    private BigDecimal maxRating;

    @Schema(description = "Minimum stock quantity")
    private Integer minStock;

    @Schema(description = "Maximum stock quantity")
    private Integer maxStock;

    @Schema(description = "Minimum available quantity")
    private Integer minAvailableQuantity;

    @Schema(description = "Filter by tags")
    private List<String> tags;

    @Schema(description = "Created after this date")
    private LocalDateTime createdAfter;

    @Schema(description = "Created before this date")
    private LocalDateTime createdBefore;

    @Schema(description = "Minimum view count")
    private Long minViews;

    @Schema(description = "Maximum view count")
    private Long maxViews;

    @Schema(description = "Minimum sales count")
    private Long minSales;

    @Schema(description = "Show only popular products (high view count)")
    private Boolean popular;

    @Schema(description = "Show only trending products (high sales)")
    private Boolean trending;

    @Schema(description = "Include category in response (eager loading)")
    @Builder.Default
    private Boolean includeCategory = true;

    @Schema(description = "Include images in response (eager loading)")
    @Builder.Default
    private Boolean includeImages = false;

    // ==================== Predicate Support ====================

    @Schema(description = "Custom filter predicate in JPQL format")
    private String customPredicate;

    @Schema(description = "Custom filter parameters as key-value pairs")
    private String customParameters;

    @Schema(description = "Sort by field")
    private String sortBy;

    @Schema(description = "Sort direction: ASC or DESC")
    private String sortDirection;

    /**
     * Check if any filter is applied
     */
    public boolean hasFilters() {
        return categoryId != null ||
                (categoryIds != null && !categoryIds.isEmpty()) ||
                categoryName != null || categorySlug != null ||
                minPrice != null || maxPrice != null ||
                keyword != null || name != null || sku != null || slug != null ||
                featured != null || isNew != null || isBestseller != null ||
                inventoryStatus != null ||
                (inventoryStatuses != null && !inventoryStatuses.isEmpty()) ||
                inStockOnly != null || lowStockOnly != null ||
                outOfStockOnly != null || needsReorderOnly != null ||
                hasDiscount != null || minDiscountPercent != null ||
                maxDiscountPercent != null ||
                minRating != null || maxRating != null ||
                minStock != null || maxStock != null ||
                minAvailableQuantity != null ||
                (tags != null && !tags.isEmpty()) ||
                createdAfter != null || createdBefore != null ||
                minViews != null || maxViews != null ||
                minSales != null || popular != null || trending != null ||
                customPredicate != null;
    }

    /**
     * Validate the filter request
     */
    public void validate() {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }

        if (minRating != null && (minRating.compareTo(BigDecimal.ZERO) < 0 ||
                minRating.compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw new IllegalArgumentException("minRating must be between 0 and 5");
        }

        if (maxRating != null && (maxRating.compareTo(BigDecimal.ZERO) < 0 ||
                maxRating.compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw new IllegalArgumentException("maxRating must be between 0 and 5");
        }

        if (minStock != null && minStock < 0) {
            throw new IllegalArgumentException("minStock cannot be negative");
        }

        if (maxStock != null && maxStock < 0) {
            throw new IllegalArgumentException("maxStock cannot be negative");
        }

        if (minDiscountPercent != null && (minDiscountPercent < 0 || minDiscountPercent > 100)) {
            throw new IllegalArgumentException("minDiscountPercent must be between 0 and 100");
        }

        if (maxDiscountPercent != null && (maxDiscountPercent < 0 || maxDiscountPercent > 100)) {
            throw new IllegalArgumentException("maxDiscountPercent must be between 0 and 100");
        }

        if (createdAfter != null && createdBefore != null &&
                createdAfter.isAfter(createdBefore)) {
            throw new IllegalArgumentException("createdAfter cannot be after createdBefore");
        }

        if (customPredicate != null && !isValidJpql(customPredicate)) {
            throw new IllegalArgumentException("Invalid JPQL custom predicate");
        }
    }

    private boolean isValidJpql(String predicate) {
        // Basic JPQL validation - prevent SQL injection
        String lowerPredicate = predicate.toLowerCase();
        return !lowerPredicate.contains("delete") &&
                !lowerPredicate.contains("insert") &&
                !lowerPredicate.contains("update") &&
                !lowerPredicate.contains("drop") &&
                !lowerPredicate.contains("alter");
    }

    /**
     * Get sort direction as Sort.Direction
     */
    public Sort.Direction getSortDirectionEnum() {
        if (sortDirection == null) {
            return Sort.Direction.ASC;
        }
        return "DESC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }



}
