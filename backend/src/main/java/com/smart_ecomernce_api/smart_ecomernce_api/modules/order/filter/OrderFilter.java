package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.filter;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderFilter {
    private Long userId;
    private String orderNumber;
    private String status;
    private String paymentStatus;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    private BigDecimal minTotalAmount;
    private BigDecimal maxTotalAmount;
    private String search;
}

