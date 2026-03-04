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
public class WishlistPriceComparisonDto {

    private Integer totalItems;

    private Double totalOriginalPrice;

    private Double totalCurrentPrice;

    private Double totalSavings;

    private Double averageDiscount;

    private List<PriceComparisonItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceComparisonItem {
        private Long productId;
        private String productName;
        private Double originalPrice;
        private Double currentPrice;
        private Double savings;
        private Double discountPercentage;
        private Boolean bestDeal;
    }
}