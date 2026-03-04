package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link WishlistItem}.
 *
 * Query design principles applied here:
 *  - Derived query methods for simple lookups (Spring Data generates optimised SQL).
 *  - JPQL @Query for anything requiring JOINs or computed predicates.
 *  - @Modifying + @Query for bulk DML (avoids loading entities just to delete/update them).
 *  - EntityGraph hints on read-heavy queries to avoid N+1 on product/user associations.
 */
@Repository
public interface WishlistRepository extends BaseRepository<WishlistItem, Long> {

    // ──────────────────────────────────────────────────────────────
    //  Core lookups
    // ──────────────────────────────────────────────────────────────

    /**
     * All items for a user, newest first.
     * The composite index idx_wishlist_user_created makes this O(log n).
     */
    @EntityGraph(attributePaths = {"product", "product.category"})
    @Query("""
           SELECT w FROM WishlistItem w
           WHERE w.user.id = :userId
           ORDER BY w.createdAt DESC
           """)
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Paginated version – Spring Data automatically rewrites into a COUNT query for the total.
     */
    @EntityGraph(attributePaths = {"product", "product.category"})
    @Query(value = """
           SELECT w FROM WishlistItem w
           WHERE w.user.id = :userId
           """,
            countQuery = "SELECT COUNT(w) FROM WishlistItem w WHERE w.user.id = :userId")
    Page<WishlistItem> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /** Single item lookup – used by update/remove/check operations. */
    @EntityGraph(attributePaths = {"product"})
    @Query("""
           SELECT w FROM WishlistItem w
           WHERE w.user.id = :userId AND w.product.id = :productId
           """)
    Optional<WishlistItem> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /** Cheap existence check – avoids loading the entity. */
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // ──────────────────────────────────────────────────────────────
    //  Price & stock queries
    // ──────────────────────────────────────────────────────────────

    /**
     * Items where the current price is lower than the price captured at add-time.
     * Sorted by the largest absolute saving first.
     */
    @Query("""
           SELECT w FROM WishlistItem w
           JOIN FETCH w.product p
           WHERE w.user.id    = :userId
             AND w.purchased  = false
             AND p.discountPrice < w.priceWhenAdded
           ORDER BY (w.priceWhenAdded - p.discountPrice) DESC
           """)
    List<WishlistItem> findItemsWithPriceDrops(@Param("userId") Long userId);

    /**
     * Bulk remove: more efficient than loading + deleting each entity.
     */
    @Modifying
    @Query("""
           DELETE FROM WishlistItem w
           WHERE w.user.id      = :userId
             AND w.product.id  IN :productIds
           """)
    int deleteByUserIdAndProductIdIn(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);

    /** Clear entire wishlist without loading items into the persistence context. */
    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);


    long countByUserId(Long userId);


    @Query("""
           SELECT w FROM WishlistItem w
           JOIN FETCH w.product
           WHERE w.user.id         = :userId
             AND w.reminderEnabled = true
             AND w.reminderDate   <= :now
             AND w.purchased       = false
           """)
    List<WishlistItem> findItemsWithDueReminders(@Param("userId") Long userId, @Param("now") LocalDateTime now);


    /**
     * Total value (sum of effective prices) and total savings for a user's unpurchased list.
     * Returns Object[] row [totalValue, totalSavings].
     */
    @Query("""
           SELECT SUM(p.discountPrice),
                  SUM(CASE WHEN w.priceWhenAdded > p.discountPrice
                           THEN w.priceWhenAdded - p.discountPrice
                           ELSE 0 END)
           FROM WishlistItem w
           JOIN w.product p
           WHERE w.user.id   = :userId
             AND w.purchased = false
           """)
    Object[] findTotalValueAndSavings(@Param("userId") Long userId);

}
