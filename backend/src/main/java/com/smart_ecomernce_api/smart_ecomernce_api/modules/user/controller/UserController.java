package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.controller;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.UserPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController("moduleUserController")
@RequestMapping("v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Operations related to user accounts")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        Optional<UserDto> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", userOpt.get()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get all users with pagination and advanced filtering")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserDto>>> getAllUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0")   int            page,
            @Parameter(description = "Page size")             @RequestParam(defaultValue = "10")  int            size,
            @Parameter(description = "Sort field")            @RequestParam(defaultValue = "id")  String         sortBy,
            @Parameter(description = "Sort direction")        @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String  search,
            @RequestParam(required = false) Role    role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        UserPredicates predicates = UserPredicates.builder()
                .withSearch(search)
                .withRole(role)
                .withActive(active)
                .withEmailVerified(emailVerified)
                .withCreatedAfter(createdAfter)
                .withCreatedBefore(createdBefore);
        Page<UserDto> users = userService.findUsersWithPredicate(predicates.build(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully",
                PaginatedResponse.from(users)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Advanced user search with QueryDSL predicates")
    public ResponseEntity<ApiResponse<PaginatedResponse<UserDto>>> searchUsers(
            @QuerydslPredicate(root = User.class) Predicate predicate,
            @RequestParam(defaultValue = "0")   int            page,
            @RequestParam(defaultValue = "10")  int            size,
            @RequestParam(defaultValue = "id")  String         sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserDto> users = userService.findUsersWithPredicate(predicate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users searched successfully",
                PaginatedResponse.from(users)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "userByEmail", allEntries = true)
    })
    @Operation(summary = "Admin: create a new user")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody UserCreateRequest request,
            UriComponentsBuilder uriBuilder) {
        UserDto created = userService.createUser(request);
        var uri = uriBuilder.path("/api/v1/users/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(uri)
                .body(ApiResponse.success("User created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "userByEmail", allEntries = true)
    })
    @Operation(summary = "Update user by ID")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated successfully",
                userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Caching(evict = {
        @CacheEvict(value = "users", allEntries = true),
        @CacheEvict(value = "userByEmail", allEntries = true)
    })
    @Operation(summary = "Delete user by ID")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully",
                userService.updateUserRole(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Update user active status")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully",
                userService.updateUserStatus(id, request)));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Change user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
