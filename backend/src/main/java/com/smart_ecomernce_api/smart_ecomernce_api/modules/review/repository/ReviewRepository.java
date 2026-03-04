package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;
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
 * Spring Data JPA repository for Review entity
 */
@Repository
public interface ReviewRepository extends BaseRepository<Review, Long> {

        /**
         * Find reviews by product and approved status
         */
        @EntityGraph(attributePaths = {"user", "product"})
        Page<Review> findByProductIdAndApproved(Long productId, Boolean approved, Pageable pageable);

        /**
         * Find reviews by user
         */
        @EntityGraph(attributePaths = {"user", "product"})
        Page<Review> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

        /**
         * Check if review exists for user and product
         */
        boolean existsByProductIdAndUserId(Long productId, Long userId);


        /**
         * Find reviews by product and rating
         */
        @EntityGraph(attributePaths = {"user", "product"})
        Page<Review> findByProductIdAndRatingAndIsActiveTrue(Long productId, Integer rating, Pageable pageable);

        /**
         * Alias for findByUserIdAndIsActiveTrue for service compatibility
         * Uses @EntityGraph to eagerly fetch user and product (N+1 fix)
         */
        default Page<Review> findByUserId(Long userId, Pageable pageable) {
                // This delegates to findByUserIdAndIsActiveTrue, which is already annotated with @EntityGraph
                return findByUserIdAndIsActiveTrue(userId, pageable);
        }

        /**
         * Alias for findByProductIdAndRatingAndIsActiveTrue for service compatibility
         */
        default Page<Review> findByProductIdAndRating(Long productId, Integer rating, Pageable pageable) {
                return findByProductIdAndRatingAndIsActiveTrue(productId, rating, pageable);
        }

        /**
         * Find reviews by product and verified purchase
         */
        @EntityGraph(attributePaths = {"user", "product"})
        Page<Review> findByProductIdAndVerifiedPurchase(Long productId, Boolean verifiedPurchase, Pageable pageable);

        /**
         * Find reviews with images
         */
        @EntityGraph(attributePaths = {"user", "product"})
        Page<Review> findByHasImagesTrueAndIsActiveTrue(Pageable pageable);

        // ==================== Custom Methods Needed for ServiceImpl ====================

        @Query("SELECT r FROM Review r WHERE r.isActive = true AND r.product.id = :productId ORDER BY r.helpfulCount DESC LIMIT :limit")
        List<Review> findMostHelpfulReviews(@Param("productId") Long productId, @Param("limit") int limit);

        @Query("SELECT r FROM Review r WHERE r.isActive = true AND r.product.id = :productId ORDER BY r.createdAt DESC LIMIT :limit")
        List<Review> findRecentReviews(@Param("productId") Long productId, @Param("limit") int limit);


        @Modifying
        @Query("UPDATE Review r SET r.approved = true WHERE r.id IN :reviewIds")
        int approveReviews(@Param("reviewIds") List<Long> reviewIds);

        @Modifying
        @Query("UPDATE Review r SET r.approved = false, r.rejectionReason = :reason WHERE r.id IN :reviewIds")
        int rejectReviews(@Param("reviewIds") List<Long> reviewIds, @Param("reason") String reason);

        @Query("SELECT COUNT(r), AVG(r.rating), SUM(CASE WHEN r.verifiedPurchase = true THEN 1 ELSE 0 END) FROM Review r WHERE r.isActive = true AND r.product.id = :productId")
        Object[] getProductRatingStats(@Param("productId") Long productId);

        @Query("SELECT r.rating, COUNT(r), (COUNT(r) * 1.0 / (SELECT COUNT(rr) FROM Review rr WHERE rr.product.id = :productId)) * 100 FROM Review r WHERE r.product.id = :productId GROUP BY r.rating")
        List<Object[]> getRatingDistributionWithPercentages(@Param("productId") Long productId);

        @Query("SELECT p FROM Review r JOIN r.pros p WHERE r.product.id = :productId GROUP BY p ORDER BY COUNT(p) DESC LIMIT :limit")
        List<String> getMostCommonPros(@Param("productId") Long productId, @Param("limit") int limit);

        @Query("SELECT c FROM Review r JOIN r.cons c WHERE r.product.id = :productId GROUP BY c ORDER BY COUNT(c) DESC LIMIT :limit")
        List<String> getMostCommonCons(@Param("productId") Long productId, @Param("limit") int limit);


        /**
         * Fetch review by id with user and product eagerly loaded (N+1 fix)
         */
        @EntityGraph(attributePaths = {"user", "product"})
        @Query("SELECT r FROM Review r WHERE r.id = :id")
        Optional<Review> findByIdWithUserAndProduct(@Param("id") Long id);

        /**
         * Find all reviews with predicate and eager fetch user and product (N+1 fix)
         */
        @EntityGraph(attributePaths = {"user", "product"})
        Page<Review> findAll(com.querydsl.core.types.Predicate predicate, Pageable pageable);

        /**
         * Find reviews with multiple filters (N+1 fix)
         */
        @EntityGraph(attributePaths = {"user", "product"})
        @Query("SELECT r FROM Review r WHERE r.isActive = true AND " +
                "(:productId IS NULL OR r.product.id = :productId) AND " +
                "(:userId IS NULL OR r.user.id = :userId) AND " +
                "(:rating IS NULL OR r.rating = :rating) AND " +
                "(:verifiedPurchase IS NULL OR r.verifiedPurchase = :verifiedPurchase) AND " +
                "(:approved IS NULL OR r.approved = :approved) AND " +
                "(:withImages IS NULL OR r.hasImages = :withImages) AND " +
                "(:dateFrom IS NULL OR r.createdAt >= :dateFrom) AND " +
                "(:dateTo IS NULL OR r.createdAt <= :dateTo)")
        Page<Review> findByFilters(
                @Param("productId") Long productId,
                @Param("userId") Long userId,
                @Param("rating") Integer rating,
                @Param("verifiedPurchase") Boolean verifiedPurchase,
                @Param("approved") Boolean approved,
                @Param("withImages") Boolean withImages,
                @Param("dateFrom") LocalDateTime dateFrom,
                @Param("dateTo") LocalDateTime dateTo,
                Pageable pageable);

        /**
         * Search reviews by comment text (N+1 fix)
         */
        @EntityGraph(attributePaths = {"user", "product"})
        @Query("SELECT r FROM Review r WHERE r.isActive = true AND " +
                "(LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                "LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Review> searchReviews(@Param("keyword") String keyword, Pageable pageable);

        /**
         * Search reviews by product and text (N+1 fix)
         */
        @EntityGraph(attributePaths = {"user", "product"})
        @Query("SELECT r FROM Review r WHERE r.isActive = true AND r.product.id = :productId AND " +
                "(LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                "LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Review> searchReviewsByProduct(@Param("productId") Long productId,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);
}
