package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.DuplicateResourceException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.mapper.UserMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.service.UserService;
import com.smart_ecomernce_api.smart_ecomernce_api.services.TokenValidationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenValidationService tokenValidationService;
    private static final String USER_NOT_FOUND = "User not found with id: ";

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return null;
        if (auth.getPrincipal() instanceof User user) return user;
        return null;
    }

    private Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void checkSelfOrAdmin(Long targetUserId) {
        Long currentUserId = getCurrentUserId();
        if (!isAdmin() && (currentUserId == null || !currentUserId.equals(targetUserId))) {
            throw new AccessDeniedException("Not authorized to access this resource");
        }
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto createUser(UserCreateRequest request) {
        if (request.getRole() != null && !request.getRole().isBlank()
                && !request.getRole().equalsIgnoreCase("USER") && !isAdmin()) {
            throw new AccessDeniedException("Only admins can assign roles");
        }
        if (userRepository.existsByUsernameAndIsActiveTrue(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmailAndIsActiveTrue(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        } else {
            user.setRole(Role.USER);
        }

        userRepository.save(user);
        log.info("User created with id: {}", user.getId());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public Optional<UserDto> getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        return Optional.of(userMapper.toDto(user));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users-page", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto updateUser(Long userId, UserUpdateRequest request) {
        checkSelfOrAdmin(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        userMapper.updateEntity(user, request);
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided for user update: {}", request.getRole());
            }
        }
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);  // profile/role may have changed
        log.info("User updated with id: {}", userId);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "users",
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public void deleteUser(Long id) {
        checkSelfOrAdmin(id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }
        userRepository.deleteById(id);
        tokenValidationService.evictPrincipal(id);   // user gone – clear cached principal
        log.info("User deleted with id: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "admin-dashboard"}, allEntries = true)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        checkSelfOrAdmin(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Password does not match");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChange(LocalDateTime.now());
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);  // password changed – stale principal must not be served
        log.info("Password changed for user with id: {}", userId);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided for user update: {}", request.getRole());
            }
        }
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);  // role changed – authorities in cached principal are stale
        log.info("User role updated for user with id: {}", userId);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto updateUserStatus(Long userId, UserStatusRequest request) {
        checkSelfOrAdmin(userId);
        if (request.getIsActive() == null) {
            throw new IllegalArgumentException("isActive is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        user.setIsActive(request.getIsActive());
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);  // isActive changed – enabled() in cached principal is stale
        log.info("User status updated for user with id: {} to isActive={}", userId, request.getIsActive());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users-predicate", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#predicate=' + #predicate.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<UserDto> findUsersWithPredicate(com.querydsl.core.types.Predicate predicate, Pageable pageable) {
        return userRepository.findAll(predicate, pageable).map(userMapper::toDto);
    }
}