package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistOptimizationRequest {

    private Double maxBudget;

    private List<String> priorityOrder; // e.g., ["URGENT", "HIGH", "MEDIUM", "LOW"]

    private Boolean includeOnlyInStock;

    private Integer maxItems;

    private String optimizationStrategy; // "PRIORITY", "PRICE", "SAVINGS", "BALANCED"
}