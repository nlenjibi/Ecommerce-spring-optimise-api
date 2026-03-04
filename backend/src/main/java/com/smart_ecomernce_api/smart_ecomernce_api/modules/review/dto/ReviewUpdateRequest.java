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
@Schema(description = "Request payload for updating a product review")
public class ReviewUpdateRequest {

    @Schema(description = "Updated star rating from 1 to 5", example = "4")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Schema(description = "Updated title")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Schema(description = "Updated comment")
    @Size(min = 10, max = 2000, message = "Comment must be between 10 and 2000 characters")
    private String comment;

    @Schema(description = "Updated list of pros")
    @Size(max = 10, message = "Maximum 10 pros allowed")
    private List<@NotBlank @Size(max = 500) String> pros;

    @Schema(description = "Updated list of cons")
    @Size(max = 10, message = "Maximum 10 cons allowed")
    private List<@NotBlank @Size(max = 500) String> cons;

    @Schema(description = "Updated image URLs")
    @Size(max = 5, message = "Maximum 5 images allowed")
    private List<@NotBlank @Pattern(regexp = "^https?://.*", message = "Invalid image URL") @Size(max = 500) String> images;
}