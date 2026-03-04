package com.smart_ecomernce_api.smart_ecomernce_api.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL Input for Category filtering
 * Supports both basic and advanced filtering options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryFilterInput {

    /**
     * Filter by category name (partial match)
     */
    private String name;

    /**
     * Filter by description (partial match)
     */
    private String description;

    /**
     * Filter by active status
     * null = all categories, true = active only, false = inactive only
     */
    private Boolean isActive;

    /**
     * Filter by featured status
     * null = all categories, true = featured only, false = non-featured only
     */
    private Boolean featured;

    /**
     * Filter categories with/without products
     * null = all categories, true = with products, false = without products
     */
    private Boolean hasProducts;

    /**
     * Minimum display order value
     */
    private Integer minOrder;

    /**
     * Maximum display order value
     */
    private Integer maxOrder;
}