package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.smart_ecomernce_api.smart_ecomernce_api.validator.ValidPriceRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidPriceRange
public class PriceDropNotificationDto {

    private Long wishlistItemId;
    private Long productId;
    private String productName;
    private String productSlug;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal priceDrop;
    private BigDecimal priceDropPercentage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime detectedAt;
}