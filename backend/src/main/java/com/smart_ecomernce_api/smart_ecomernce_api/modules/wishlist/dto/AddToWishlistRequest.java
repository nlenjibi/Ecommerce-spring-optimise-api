package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto;



import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.entity.WishlistPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class AddToWishlistRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    private WishlistPriority priority;

    @Min(value = 1, message = "Desired quantity must be at least 1")
    private Integer desiredQuantity;

    @Positive(message = "Target price must be positive")
    private BigDecimal targetPrice;

    private Boolean notifyOnPriceDrop;

    private Boolean notifyOnStock;

    private Boolean isPublic;

    private String collectionName;
}
