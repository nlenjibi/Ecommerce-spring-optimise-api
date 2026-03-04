package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor  // Add this - required for Jackson deserialization
@Data
@Getter
@Setter
public class UserUpdateRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email should be valid")
    private String email;

    private String firstName;
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number format is invalid")
    private String phoneNumber;

    private String role;
    private Boolean isActive;
}
