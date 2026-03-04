package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for review operations
 * Provides business logic for managing product reviews
 */
public interface ReviewService {

    // ==================== Basic CRUD Operations ====================

    ReviewResponse createReview(ReviewCreateRequest request, Long userId);

    ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request, Long userId);

    void deleteReview(Long reviewId, Long userId);

    ReviewResponse getReview(Long reviewId);

    ReviewResponse restoreReview(Long reviewId, Long userId);

    // ==================== Query Operations ====================

    Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    Page<ReviewResponse> getProductReviewsWithFilters(Long productId, ReviewFilterRequest filters, Pageable pageable);

    // ==================== Statistics & Analytics ====================

    ReviewSummaryResponse getProductRatingStats(Long productId);

    // ==================== Voting Operations ====================

    void markHelpful(Long reviewId);


    // ==================== Admin Operations ====================

    ReviewResponse approveReview(Long reviewId);

    ReviewResponse rejectReview(Long reviewId, String reason);

    ReviewResponse addAdminResponse(Long reviewId, AdminResponseRequest request, Long adminId);

    ReviewResponse removeAdminResponse(Long reviewId);


    int bulkApproveReviews(List<Long> reviewIds);

    int bulkRejectReviews(List<Long> reviewIds, String reason);

    // ==================== Utility Operations ====================



    /**
     * Find reviews with QueryDSL predicate for advanced filtering
     */
    Page<ReviewResponse> findReviewsWithPredicate(Predicate predicate, Pageable pageable);
}
