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
public class WishlistAnalyticsDto {

    private Long userId;

    private Integer totalItems;

    private Integer itemsAddedThisMonth;

    private Integer itemsPurchased;

    private Integer itemsWithPriceDrops;

    private Double averagePriceDrop;

    private Double totalSavings;

    private String mostAddedCategory;

    private String highestPriorityCategory;

    private Integer averageDaysInWishlist;

    private List<CategoryAnalytics> categoryBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAnalytics {
        private String categoryName;
        private Integer itemCount;
        private Double totalValue;
        private Double averagePrice;
    }
}