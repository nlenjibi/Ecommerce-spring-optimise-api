package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for adding admin response to a review")
public class AdminResponseRequest {

    @Schema(description = "Admin's response message", required = true)
    @NotBlank(message = "Response is required")
    @Size(min = 10, max = 1000, message = "Response must be between 10 and 1000 characters")
    private String response;
}