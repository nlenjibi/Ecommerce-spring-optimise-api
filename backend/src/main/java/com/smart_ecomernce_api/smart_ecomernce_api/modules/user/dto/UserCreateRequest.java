package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.validator.Lowercase;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserCreateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Lowercase
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Lowercase
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Pattern(regexp = "^[+]?\\d{10,15}$", message = "Phone number format is invalid")
    private String phoneNumber;

    private String role;
}