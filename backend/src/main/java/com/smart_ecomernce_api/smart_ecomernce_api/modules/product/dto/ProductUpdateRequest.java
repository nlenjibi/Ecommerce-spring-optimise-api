package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {

    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Discounted price cannot be negative")
    private BigDecimal discountedPrice;

    @Positive(message = "Stock quantity must be positive")
    private Integer stockQuantity;

    @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
    private String sku;

    @Positive(message = "Category ID must be positive")
    private Long categoryId;

    private Boolean isActive;

    private String imageUrl;

    private java.util.List<String> imageUrls;
}