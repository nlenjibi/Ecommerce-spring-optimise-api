package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Data
public class UserLoginRequest {
    private String usernameOrEmail;
    private String password;
}
