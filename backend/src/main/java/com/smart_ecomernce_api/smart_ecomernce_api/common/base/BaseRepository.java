package com.smart_ecomernce_api.smart_ecomernce_api.common.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Base repository interface that extends JpaRepository and QuerydslPredicateExecutor
 * with common methods for all entities in the e-commerce system.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key type
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID>, QuerydslPredicateExecutor<T> {

    Page<T> findByIsActiveTrue(Pageable pageable);

    /**
     * Find entity by ID if it's active
     */
    Optional<T> findByIdAndIsActiveTrue(ID id);

    /**
     * Find entities created between dates with pagination
     */
    Page<T> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find entities updated between dates with pagination
     */
    Page<T> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Check if active entity exists by ID
     */
    boolean existsByIdAndIsActiveTrue(ID id);

    /**
     * Count all active entities
     */
    long countByIsActiveTrue();
}
