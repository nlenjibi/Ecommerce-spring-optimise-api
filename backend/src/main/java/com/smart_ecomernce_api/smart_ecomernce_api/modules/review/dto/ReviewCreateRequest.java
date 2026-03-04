package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating a product review")
public class ReviewCreateRequest {

    @Schema(description = "ID of the product being reviewed", example = "123", required = true)
    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    @Schema(description = "Star rating from 1 to 5", example = "5", required = true)
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Schema(description = "Short title for the review", example = "Amazing product!")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Schema(description = "Detailed review comment", example = "This product exceeded my expectations...", required = true)
    @NotBlank(message = "Comment is required")
    @Size(min = 10, max = 2000, message = "Comment must be between 10 and 2000 characters")
    private String comment;

    @Schema(description = "List of positive aspects")
    @Size(max = 10, message = "Maximum 10 pros allowed")
    private List<@NotBlank @Size(max = 500) String> pros;

    @Schema(description = "List of negative aspects")
    @Size(max = 10, message = "Maximum 10 cons allowed")
    private List<@NotBlank @Size(max = 500) String> cons;

    @Schema(description = "URLs of review images")
    @Size(max = 5, message = "Maximum 5 images allowed")
    private List<@NotBlank @Pattern(regexp = "^https?://.*", message = "Invalid image URL") @Size(max = 500) String> images;
}