package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderExportResponse {
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime orderDate;
}