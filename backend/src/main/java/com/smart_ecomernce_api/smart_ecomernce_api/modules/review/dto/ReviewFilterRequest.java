package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Filters for querying reviews")
public class ReviewFilterRequest {

    @Schema(description = "Filter by rating (1-5)")
    @Min(1)
    @Max(5)
    private Integer rating;

    @Schema(description = "Filter by verified purchase only")
    private Boolean verifiedPurchase;

    @Schema(description = "Filter by approval status")
    private Boolean approved;

    @Schema(description = "Filter reviews with images only")
    private Boolean withImages;

    @Schema(description = "Minimum helpful votes")
    @Min(0)
    private Integer minHelpfulVotes;

    @Schema(description = "Date from (ISO format)")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime dateFrom;

    @Schema(description = "Date to (ISO format)")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime dateTo;
}