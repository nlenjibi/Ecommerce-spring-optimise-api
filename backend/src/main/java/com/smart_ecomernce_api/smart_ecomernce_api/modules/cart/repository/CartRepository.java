package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Cart entity
 */
@Repository
public interface CartRepository extends BaseRepository<Cart, Long> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.id = :id")
    Optional<Cart> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT c FROM Cart c WHERE c.createdAt < :cutoffDate AND SIZE(c.items) > 0")
    List<Cart> findAbandonedCartsBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.status = :status")
    Optional<Cart> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CartStatus status);

}
