package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.Role;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity
 */
@Repository
public interface UserRepository extends BaseRepository<User, Long> {


    boolean existsByUsernameAndIsActiveTrue(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmailAndIsActiveTrue(String email);


    /**
     * Find all users matching a QueryDSL Predicate (for advanced filtering)
     */
    Page<User> findAll(Predicate predicate, Pageable pageable);

    /**
     * Count active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

    /**
     * Count inactive users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = false")
    long countInactiveUsers();

    /**
     * Deactivate user (soft delete)
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = false, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int deactivateUser(@Param("userId") Long userId);


    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String username);
}