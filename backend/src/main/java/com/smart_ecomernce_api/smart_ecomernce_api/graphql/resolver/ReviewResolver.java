package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.entity.ReviewPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto.ReviewResponseDto;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.ReviewFilterInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewSummaryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.dto.ReviewUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL resolver for Review queries and mutations.
 *
 * Security model
 * ──────────────
 * Reads (productReviews, productRatingStats, searchReviews) are public —
 * shoppers browse reviews without logging in.
 *
 * Writes (createReview, updateReview, deleteReview, markReviewHelpful) require
 * authentication. The ReviewService enforces ownership: users may only modify
 * their own reviews.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
class ReviewResolver {

    private final ReviewService reviewService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Queries – public (requireAuth = false)
    // ─────────────────────────────────────────────────────────────────────────

    @QueryMapping
    
    public ReviewResponseDto productReviews(
            @Argument Long productId,
            @Argument PageInput pagination,
            @Argument ReviewFilterInput filter) {
        log.info("GraphQL Query: productReviews productId={}", productId);
        Pageable pageable = buildPageable(pagination);
        Page<ReviewResponse> page = filter != null
                ? reviewService.findReviewsWithPredicate(buildProductPredicate(productId, filter), pageable)
                : reviewService.getProductReviews(productId, pageable);
        return toDto(page);
    }

    @QueryMapping
    
    public ReviewSummaryResponse productRatingStats(@Argument Long productId) {
        log.info("GraphQL Query: productRatingStats productId={}", productId);
        return reviewService.getProductRatingStats(productId);
    }

    @QueryMapping
    
    public ReviewResponseDto searchReviews(
            @Argument ReviewFilterInput filter,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: searchReviews");
        Page<ReviewResponse> page = reviewService.findReviewsWithPredicate(
                buildGeneralPredicate(filter), buildPageable(pagination));
        return toDto(page);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Mutations – any authenticated user (service enforces ownership)
    // ─────────────────────────────────────────────────────────────────────────

    @MutationMapping
    
    public ReviewResponse createReview(
            @Argument ReviewCreateRequest input,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: createReview user={}", userId);
        return reviewService.createReview(input, userId);
    }

    @MutationMapping
    
    public ReviewResponse updateReview(
            @Argument Long id,
            @Argument ReviewUpdateRequest input,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: updateReview id={} user={}", id, userId);
        return reviewService.updateReview(id, input, userId);
    }

    @MutationMapping
    
    public Boolean deleteReview(
            @Argument Long id,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: deleteReview id={} user={}", id, userId);
        reviewService.deleteReview(id, userId);
        return true;
    }

    @MutationMapping
    
    public ReviewResponse markReviewHelpful(@Argument Long id) {
        log.info("GraphQL Mutation: markReviewHelpful id={}", id);
        reviewService.markHelpful(id);
        return reviewService.getReview(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ReviewResponseDto toDto(Page<ReviewResponse> page) {
        return ReviewResponseDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    private Pageable buildPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }

    private Predicate buildProductPredicate(Long productId, ReviewFilterInput f) {
        ReviewPredicates p = ReviewPredicates.builder()
                .withProductId(productId)
                .withRating(f.getRating())
                .withMinRating(f.getMinRating())
                .withMaxRating(f.getMaxRating())
                .withVerifiedPurchase(f.getVerifiedPurchase())
                .withApproved(f.getApproved())
                .withImages(f.getWithImages())
                .withTextContaining(f.getSearchText())
                .withCreatedAfter(f.getDateFrom())
                .withCreatedBefore(f.getDateTo());
        if (Boolean.TRUE.equals(f.getPositiveOnly()))   p.withPositiveRating();
        if (Boolean.TRUE.equals(f.getNegativeOnly()))   p.withNegativeRating();
        if (Boolean.TRUE.equals(f.getNeedsAttention())) p.withNeedsAttention();
        return p.build();
    }

    private Predicate buildGeneralPredicate(ReviewFilterInput f) {
        ReviewPredicates p = ReviewPredicates.builder()
                .withProductId(f.getProductId())
                .withUserId(f.getUserId())
                .withRating(f.getRating())
                .withMinRating(f.getMinRating())
                .withMaxRating(f.getMaxRating())
                .withVerifiedPurchase(f.getVerifiedPurchase())
                .withApproved(f.getApproved())
                .withImages(f.getWithImages())
                .withTextContaining(f.getSearchText())
                .withCreatedAfter(f.getDateFrom())
                .withCreatedBefore(f.getDateTo());
        if (Boolean.TRUE.equals(f.getPositiveOnly()))   p.withPositiveRating();
        if (Boolean.TRUE.equals(f.getNegativeOnly()))   p.withNegativeRating();
        if (Boolean.TRUE.equals(f.getNeedsAttention())) p.withNeedsAttention();
        return p.buildActiveOnly();
    }
}