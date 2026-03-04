package com.smart_ecomernce_api.smart_ecomernce_api.graphql.input;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GraphQL Input for User filtering using Specifications
 */
@Data
public class UserFilterInput {
    private String search;
    private Role role;
    private Boolean active;
    private Boolean emailVerified;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private String phoneNumber;
    private String name;
}
