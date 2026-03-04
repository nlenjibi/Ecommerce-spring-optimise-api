package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for approving or rejecting a review")
public class ReviewApprovalRequest {

    @Schema(description = "Whether to approve the review", example = "true", required = true)
    @NotNull(message = "Approval status is required")
    private Boolean approved;

    @Schema(description = "Reason for rejection (required if approved=false)")
    private String rejectionReason;
}