package com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.dto;




import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Used for lock/unlock endpoints.
 * Carries sensitive admin action data in the request body, not query params.
 */
@Data
public class AccountActionRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    // Optional for unlock, required validation enforced at controller level for lock
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}