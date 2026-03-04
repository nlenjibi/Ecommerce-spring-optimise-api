package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    UserDto createUser(UserCreateRequest request);
    Optional<UserDto> getUserById(Long id);

    Page<UserDto> getAllUsers(Pageable pageable);
    UserDto updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);

    void changePassword(Long userId, ChangePasswordRequest request);
    UserDto updateUserRole(Long userId, UpdateUserRoleRequest request);
    UserDto updateUserStatus(Long userId, UserStatusRequest request);


    // Advanced querying with predicates
    Page<UserDto> findUsersWithPredicate(Predicate predicate, Pageable pageable);
}
