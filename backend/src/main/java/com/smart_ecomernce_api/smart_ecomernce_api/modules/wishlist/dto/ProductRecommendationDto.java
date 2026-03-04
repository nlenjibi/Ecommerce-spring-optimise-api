package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecommendationDto {

    private Long productId;

    private String productName;

    private String imageUrl;

    private Double price;

    private String recommendationReason;

    private Double similarityScore;

    private List<String> matchingTags;
}