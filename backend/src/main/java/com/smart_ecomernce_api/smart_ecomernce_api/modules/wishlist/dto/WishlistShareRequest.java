package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistShareRequest {

    private String shareName;

    private String description;

    private Boolean allowPurchaseTracking;

    private Boolean showPrices;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    private List<Long> productIds; // specific products to share, null for all

    private String password; // optional password protection
}
