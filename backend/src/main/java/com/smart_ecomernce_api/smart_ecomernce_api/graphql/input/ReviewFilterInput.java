package com.smart_ecomernce_api.smart_ecomernce_api.graphql.input;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * GraphQL Input for Review filtering using Specifications
 */
@Data
public class ReviewFilterInput {
    private Long productId;
    private Long userId;
    private Integer rating;
    private Integer minRating;
    private Integer maxRating;
    private Boolean verifiedPurchase;
    private Boolean approved;
    private Boolean withImages;
    private String searchText;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private Boolean positiveOnly;
    private Boolean negativeOnly;
    private Boolean needsAttention;
}
