package com.smart_ecomernce_api.smart_ecomernce_api.graphql.input;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductFilterRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GraphQL Product Filter Input
 * Matches ProductFilterRequest from REST API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterInput {

    // ==================== Category Filters ====================

    private Long categoryId;
    private List<Long> categoryIds;
    private String categoryName;
    private String categorySlug;

    // ==================== Price Filters ====================

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean hasDiscount;
    private Integer minDiscountPercent;
    private Integer maxDiscountPercent;

    // ==================== Search Filters ====================

    private String keyword;
    private String name;
    private String sku;
    private String slug;

    // ==================== Marketing Filters ====================

    private Boolean featured;
    private Boolean isNew;
    private Boolean isBestseller;

    // ==================== Inventory Filters ====================

    private InventoryStatus inventoryStatus;
    private List<InventoryStatus> inventoryStatuses;
    private Boolean inStockOnly;
    private Boolean lowStockOnly;
    private Boolean outOfStockOnly;
    private Boolean needsReorderOnly;

    // ==================== Stock Quantity Filters ====================

    private Integer minStock;
    private Integer maxStock;
    private Integer minAvailableQuantity;

    // ==================== Rating Filters ====================

    private BigDecimal minRating;
    private BigDecimal maxRating;

    // ==================== Tag Filters ====================

    private List<String> tags;

    // ==================== Date Filters ====================

    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    // ==================== View/Sales Filters ====================

    private Long minViews;
    private Long maxViews;
    private Long minSales;
    private Boolean popular;
    private Boolean trending;

    // ==================== Eager Loading Options ====================

    @Builder.Default
    private Boolean includeCategory = true;

    @Builder.Default
    private Boolean includeImages = false;

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
                minSales != null || popular != null || trending != null;
    }

    /**
     * Convert to ProductFilterRequest for service layer
     */
    public ProductFilterRequest toFilterRequest() {
        return ProductFilterRequest.builder()
                .categoryId(categoryId)
                .categoryIds(categoryIds)
                .categoryName(categoryName)
                .categorySlug(categorySlug)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .hasDiscount(hasDiscount)
                .minDiscountPercent(minDiscountPercent)
                .maxDiscountPercent(maxDiscountPercent)
                .keyword(keyword)
                .name(name)
                .sku(sku)
                .slug(slug)
                .featured(featured)
                .isNew(isNew)
                .isBestseller(isBestseller)
                .inventoryStatus(inventoryStatus)
                .inventoryStatuses(inventoryStatuses)
                .inStockOnly(inStockOnly)
                .lowStockOnly(lowStockOnly)
                .outOfStockOnly(outOfStockOnly)
                .needsReorderOnly(needsReorderOnly)
                .minStock(minStock)
                .maxStock(maxStock)
                .minAvailableQuantity(minAvailableQuantity)
                .minRating(minRating)
                .maxRating(maxRating)
                .tags(tags)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .minViews(minViews)
                .maxViews(maxViews)
                .minSales(minSales)
                .popular(popular)
                .trending(trending)
                .includeCategory(includeCategory)
                .includeImages(includeImages)
                .build();
    }
}