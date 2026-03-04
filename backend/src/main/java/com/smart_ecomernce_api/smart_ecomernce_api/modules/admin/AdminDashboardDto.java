package com.smart_ecomernce_api.smart_ecomernce_api.modules.admin;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AdminDashboardDto {
    private Long totalUsers;
    private Long totalOrders;
    private Long totalProducts;
    private BigDecimal totalRevenue;
    private Long pendingOrders;
    private Long activeUsers;
    private Long lowStockProducts; // Added field for low stock products
}