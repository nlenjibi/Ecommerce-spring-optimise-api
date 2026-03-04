package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto;


import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class WishlistSummaryDto {

    private Long userId;
    private Integer totalItems;
    private Integer inStockItems;
    private Integer outOfStockItems;
    private Integer itemsWithPriceDrops;
    private Integer purchasedItems;

    private BigDecimal totalValue;
    private BigDecimal totalSavings;

    private List<WishlistItemDto> items;
}
