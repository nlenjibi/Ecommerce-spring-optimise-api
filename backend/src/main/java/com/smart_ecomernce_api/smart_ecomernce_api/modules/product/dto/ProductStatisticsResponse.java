package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatisticsResponse {
    private Long totalProducts;
    private Long activeProducts;
    private Long featuredProducts;
    private Long outOfStockProducts;
    private Long lowStockProducts;
    private BigDecimal averagePrice;
    private BigDecimal totalInventoryValue;
    private Map<String, Long> productsByCategory;
    private Map<String, Long> productsByStatus;
}
