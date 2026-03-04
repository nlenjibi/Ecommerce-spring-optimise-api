package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import java.time.LocalDateTime;

/**
 * QueryDSL Predicate builder for User entity
 * Provides type-safe, compile-time checked queries for complex user filtering
 * 
 * Usage:
 * <pre>
 * Predicate predicate = UserPredicates.builder()
 *     .withUsernameContaining("john")
 *     .withRole(Role.USER)
 *     .withCreatedAfter(LocalDateTime.now().minusDays(30))
 *     .build();
 * 
 * userRepository.findAll(predicate, pageable);
 * </pre>
 */
public class UserPredicates {

    private final BooleanBuilder builder;

    public UserPredicates() {
        this.builder = new BooleanBuilder();
    }

    /**
     * Create a new predicate builder
     */
    public static UserPredicates builder() {
        return new UserPredicates();
    }



    /**
     * Filter by name containing (first OR last name)
     */
    public UserPredicates withNameContaining(String name) {
        if (name != null && !name.isEmpty()) {
            builder.and(
                Expressions.stringPath("firstName").containsIgnoreCase(name)
                    .or(Expressions.stringPath("lastName").containsIgnoreCase(name))
            );
        }
        return this;
    }


    /**
     * Filter by exact role
     */
    public UserPredicates withRole(Role role) {
        if (role != null) {
            builder.and(Expressions.enumPath(Role.class, "role").eq(role));
        }
        return this;
    }

    /**
     * Filter by phone number containing
     */
    public UserPredicates withPhoneNumberContaining(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            builder.and(Expressions.stringPath("phoneNumber").contains(phoneNumber));
        }
        return this;
    }

    /**
     * Filter users created after date
     */
    public UserPredicates withCreatedAfter(LocalDateTime date) {
        if (date != null) {
            builder.and(Expressions.dateTimePath(LocalDateTime.class, "createdAt")
                .goe(date));
        }
        return this;
    }

    /**
     * Filter users created before date
     */
    public UserPredicates withCreatedBefore(LocalDateTime date) {
        if (date != null) {
            builder.and(Expressions.dateTimePath(LocalDateTime.class, "createdAt")
                .loe(date));
        }
        return this;
    }


    /**
     * Filter by email verified status
     */
    public UserPredicates withEmailVerified(Boolean verified) {
        if (verified != null) {
            builder.and(Expressions.booleanPath("emailVerified").eq(verified));
        }
        return this;
    }

    /**
     * Filter active users only
     */
    public UserPredicates withActive(Boolean active) {
        if (active != null) {
            builder.and(Expressions.booleanPath("isActive").eq(active));
        } else {
            // Default to active only
            builder.and(Expressions.booleanPath("isActive").isTrue());
        }
        return this;
    }

    /**
     * Complex search across multiple fields
     */
    public UserPredicates withSearch(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            builder.and(
                Expressions.stringPath("username").containsIgnoreCase(keyword)
                    .or(Expressions.stringPath("email").containsIgnoreCase(keyword))
                    .or(Expressions.stringPath("firstName").containsIgnoreCase(keyword))
                    .or(Expressions.stringPath("lastName").containsIgnoreCase(keyword))
            );
        }
        return this;
    }

    /**
     * Build the predicate
     */
    public Predicate build() {
        return builder.getValue();
    }


}
