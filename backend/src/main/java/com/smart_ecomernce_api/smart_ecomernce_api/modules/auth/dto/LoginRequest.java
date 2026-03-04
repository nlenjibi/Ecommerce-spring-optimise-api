package com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.validator.Lowercase;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Lowercase
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

