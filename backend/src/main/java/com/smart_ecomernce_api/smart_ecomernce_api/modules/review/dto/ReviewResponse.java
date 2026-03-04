package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Review response with full details")
public class ReviewResponse {

    @Schema(description = "Review ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "123")
    private Long productId;

    private String productSlug;


    @Schema(description = "Product name", example = "Wireless Headphones")
    private String productName;

    @Schema(description = "User information")
    private UserInfo user;

    @Schema(description = "Star rating (1-5)", example = "5")
    private Integer rating;

    @Schema(description = "Review title", example = "Great product!")
    private String title;

    @Schema(description = "Review comment")
    private String comment;

    @Schema(description = "Whether this is a verified purchase")
    private Boolean verifiedPurchase;

    @Schema(description = "Whether the review is approved by admin")
    private Boolean approved;

    @Schema(description = "Number of helpful votes", example = "15")
    private Integer helpfulCount;

    @Schema(description = "Number of not helpful votes", example = "2")
    private Integer notHelpfulCount;

    @Schema(description = "Helpfulness percentage", example = "88.24")
    private Double helpfulPercentage;

    @Schema(description = "Total votes", example = "17")
    private Integer totalVotes;

    @Schema(description = "Review images")
    private List<String> images;

    @Schema(description = "Positive aspects")
    private List<String> pros;

    @Schema(description = "Negative aspects")
    private List<String> cons;

    @Schema(description = "Admin response to the review")
    private String adminResponse;

    @Schema(description = "When admin responded")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime adminResponseAt;

    @Schema(description = "Rejection reason if not approved")
    private String rejectionReason;

    @Schema(description = "Creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User information")
    public static class UserInfo {
        @Schema(description = "User ID", example = "456")
        private Long id;

        @Schema(description = "User's first name", example = "John")
        private String firstName;

        @Schema(description = "User's last name", example = "Doe")
        private String lastName;

        @Schema(description = "User's email", example = "john.doe@example.com")
        private String email;

        @Schema(description = "User's avatar URL")
        private String avatarUrl;

        @Schema(description = "User's total review count", example = "12")
        private Integer totalReviews;
    }
}