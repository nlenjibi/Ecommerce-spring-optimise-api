package com.smart_ecomernce_api.smart_ecomernce_api.modules.review.controller;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ReviewPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.Review;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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

import java.time.LocalDateTime;

/**
 * Product review REST controller.
 *
 * Security model
 * ──────────────
 * Public  (requireAuth = false) → GET /v1/reviews, GET /v1/reviews/{id},
 *   GET /v1/reviews/search, GET /v1/reviews/product/**, GET /v1/reviews/product/{id}/stats
 * Authenticated (any user)     → POST, PUT, DELETE (own reviews only – service enforces ownership)
 * ADMIN / MANAGER              → admin-response endpoints
 */
@RestController
@RequestMapping("v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "APIs for managing product reviews, ratings and analytics")
public class ReviewController {

    private final ReviewService reviewService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Public reads
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{reviewId}")

    @Operation(summary = "Get review by ID")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReview(reviewId)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get all reviews with filtering (no search)")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0")         int     page,
            @RequestParam(defaultValue = "10")        int     size,
            @RequestParam(defaultValue = "createdAt") String  sortBy,
            @RequestParam(defaultValue = "DESC")      String  direction,
            @RequestParam(required = false) Long      productId,
            @RequestParam(required = false) Long      userId,
            @RequestParam(required = false) Integer   rating,
            @RequestParam(required = false) Integer   minRating,
            @RequestParam(required = false) Integer   maxRating,
            @RequestParam(required = false) Boolean   verifiedPurchase,
            @RequestParam(required = false) Boolean   approved,
            @RequestParam(required = false) Boolean   withImages,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(direction), sortBy));
        ReviewPredicates predicates = ReviewPredicates.builder()
                .withProductId(productId)
                .withUserId(userId)
                .withRating(rating)
                .withMinRating(minRating)
                .withMaxRating(maxRating)
                .withVerifiedPurchase(verifiedPurchase)
                .withApproved(approved)
                .withImages(withImages)
                .withCreatedAfter(createdAfter)
                .withCreatedBefore(createdBefore);
        Predicate finalPredicate = predicates.buildActiveOnly();
        Page<ReviewResponse> reviews = reviewService.findReviewsWithPredicate(
                finalPredicate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched successfully",
                PaginatedResponse.from(reviews)));
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('ADMIN')")

    @Operation(summary = "Search reviews with searchText and advanced predicate")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> searchReviews(
            @RequestParam(defaultValue = "0")         int     page,
            @RequestParam(defaultValue = "10")        int     size,
            @RequestParam(defaultValue = "createdAt") String  sortBy,
            @RequestParam(defaultValue = "DESC")      String  direction,
            @RequestParam(required = false) String    searchText,
            @QuerydslPredicate(root = Review.class) Predicate predicate
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(direction), sortBy));
        Predicate finalPredicate;
        if (predicate != null) {
            finalPredicate = predicate;
        } else if (searchText != null && !searchText.isBlank()) {
            ReviewPredicates predicates = ReviewPredicates.builder()
                    .withTextContaining(searchText);
            finalPredicate = predicates.buildActiveOnly();
        } else {
            finalPredicate = null;
        }
        Page<ReviewResponse> reviews = reviewService.findReviewsWithPredicate(
                finalPredicate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reviews search successful",
                PaginatedResponse.from(reviews)));
    }

    @GetMapping("/product/{productId}")

    @Operation(summary = "Get reviews for a product")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC")      String direction) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(direction), sortBy));
        return ResponseEntity.ok(ApiResponse.success(
                PaginatedResponse.from(reviewService.getProductReviews(productId, pageable))));
    }

    @PostMapping("/product/{productId}/filter")

    @Operation(summary = "Get filtered reviews for a product")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getFilteredReviews(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewFilterRequest filters,
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC")      String direction) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(direction), sortBy));
        return ResponseEntity.ok(ApiResponse.success(
                PaginatedResponse.from(
                        reviewService.getProductReviewsWithFilters(productId, filters, pageable))));
    }

    @GetMapping("/product/{productId}/stats")

    @Operation(summary = "Get product rating statistics")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getProductStats(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getProductRatingStats(productId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Authenticated user writes (service enforces ownership)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping

    @Operation(summary = "Create a product review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @RequestParam(required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User ID is required to create a review", null));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully",
                        reviewService.createReview(request, userId)));
    }

    @PutMapping("/{reviewId}")

    @Operation(summary = "Update your review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully",
                reviewService.updateReview(reviewId, request, userId)));
    }

    @DeleteMapping("/{reviewId}")

    @Operation(summary = "Delete your review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            @RequestParam(required = false) Long userId) {
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    @PutMapping("/{reviewId}/restore")

    @Operation(summary = "Restore a soft-deleted review")
    public ResponseEntity<ApiResponse<ReviewResponse>> restoreReview(
            @PathVariable Long reviewId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Review restored successfully",
                reviewService.restoreReview(reviewId, userId)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Admin endpoints – ADMIN / MANAGER only
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{reviewId}/admin-response")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Add admin response to a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> addAdminResponse(
            @PathVariable @Positive Long reviewId,
            @Valid @RequestBody AdminResponseRequest request,
            @RequestParam(required = false) Long adminId) {
        return ResponseEntity.ok(ApiResponse.success("Admin response added successfully",
                reviewService.addAdminResponse(reviewId, request, adminId)));
    }

    @DeleteMapping("/admin/{reviewId}/admin-response")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Remove admin response from a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> removeAdminResponse(
            @PathVariable @Positive Long reviewId) {
        return ResponseEntity.ok(ApiResponse.success("Admin response removed successfully",
                reviewService.removeAdminResponse(reviewId)));
    }

    @PutMapping("/admin/{reviewId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Approve a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(
            @PathVariable @Positive Long reviewId) {
        return ResponseEntity.ok(ApiResponse.success("Review approved successfully",
                reviewService.approveReview(reviewId)));
    }

    @PutMapping("/admin/{reviewId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Reject a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(
            @PathVariable @Positive Long reviewId,
            @RequestBody RejectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review rejected successfully",
                reviewService.rejectReview(reviewId, request.getReason())));
    }

    @PostMapping("/admin/bulk-approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Bulk approve reviews")
    public ResponseEntity<ApiResponse<Integer>> bulkApproveReviews(
            @RequestBody BulkReviewActionRequest request) {
        int count = reviewService.bulkApproveReviews(request.getIds());
        return ResponseEntity.ok(ApiResponse.success(count + " reviews approved successfully", count));
    }

    @PostMapping("/admin/bulk-reject")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Bulk reject reviews")
    public ResponseEntity<ApiResponse<Integer>> bulkRejectReviews(
            @RequestBody BulkReviewActionRequest request) {
        int count = reviewService.bulkRejectReviews(request.getIds(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success(count + " reviews rejected successfully", count));
    }
}
