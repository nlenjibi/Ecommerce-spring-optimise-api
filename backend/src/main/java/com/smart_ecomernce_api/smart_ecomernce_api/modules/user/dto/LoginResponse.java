package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Data
public class LoginResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String sessionToken; // Simple session/JWT token placeholder
}
