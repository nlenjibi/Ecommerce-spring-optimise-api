package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Review statistics summary")
public class ReviewSummaryResponse {

    @Schema(description = "Total number of reviews", example = "150")
    private Long totalReviews;

    @Schema(description = "Average rating", example = "4.5")
    private Double averageRating;

    @Schema(description = "Number of verified purchases", example = "120")
    private Long verifiedPurchases;

    @Schema(description = "Percentage of verified purchases", example = "80.0")
    private Double verifiedPurchasePercentage;

    @Schema(description = "Rating distribution")
    private RatingDistribution distribution;

    @Schema(description = "Most common pros")
    private List<String> topPros;

    @Schema(description = "Most common cons")
    private List<String> topCons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Distribution of ratings")
    public static class RatingDistribution {
        @Schema(description = "Count of 5-star reviews", example = "80")
        private Long fiveStars;

        @Schema(description = "Percentage of 5-star reviews", example = "53.33")
        private Double fiveStarsPercentage;

        @Schema(description = "Count of 4-star reviews", example = "40")
        private Long fourStars;

        @Schema(description = "Percentage of 4-star reviews", example = "26.67")
        private Double fourStarsPercentage;

        @Schema(description = "Count of 3-star reviews", example = "20")
        private Long threeStars;

        @Schema(description = "Percentage of 3-star reviews", example = "13.33")
        private Double threeStarsPercentage;

        @Schema(description = "Count of 2-star reviews", example = "7")
        private Long twoStars;

        @Schema(description = "Percentage of 2-star reviews", example = "4.67")
        private Double twoStarsPercentage;

        @Schema(description = "Count of 1-star reviews", example = "3")
        private Long oneStar;

        @Schema(description = "Percentage of 1-star reviews", example = "2.0")
        private Double oneStarPercentage;
    }
}