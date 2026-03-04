package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto.WishListItemResponseDto;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.UpdateWishlistItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistSummaryDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL resolver for Wishlist queries and mutations.
 *
 * Security model
 * ──────────────
 * Every operation requires authentication — @RequestValidation with no roles
 * means any authenticated user is permitted.
 *
 * The authenticated user's ID is always sourced from @ContextValue (injected by
 * AuthenticationFilter from the validated token), never from a caller-supplied
 * argument. This prevents one user from reading or mutating another user's wishlist.
 * The WishlistService also enforces ownership as a second line of defence.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "wishlists")
class WishlistResolver {

    private final WishlistService wishlistService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Queries – any authenticated user
    // ─────────────────────────────────────────────────────────────────────────

    @QueryMapping
    
    public List<WishlistItemDto> myWishlist(@ContextValue Long userId) {
        log.info("GraphQL Query: myWishlist user={}", userId);
        return wishlistService.getUserWishlist(userId);
    }

    @QueryMapping
    
    public WishListItemResponseDto myWishlistPaginated(
            @Argument PageInput pagination,
            @ContextValue Long userId) {
        log.info("GraphQL Query: myWishlistPaginated user={}", userId);
        Page<WishlistItemDto> page = wishlistService.getUserWishlistPaginated(userId, buildPageable(pagination));
        return WishListItemResponseDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    
    public WishlistSummaryDto wishlistSummary(@ContextValue Long userId) {
        log.info("GraphQL Query: wishlistSummary user={}", userId);
        return wishlistService.getWishlistSummary(userId);
    }

    @QueryMapping
    
    public Boolean isInWishlist(@Argument Long productId, @ContextValue Long userId) {
        log.info("GraphQL Query: isInWishlist productId={} user={}", productId, userId);
        return wishlistService.isInWishlist(userId, productId);
    }

    @QueryMapping
    
    public List<WishlistItemDto> wishlistItemsWithPriceDrops(@ContextValue Long userId) {
        log.info("GraphQL Query: wishlistItemsWithPriceDrops user={}", userId);
        return wishlistService.getItemsWithPriceDrops(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Mutations – any authenticated user
    // ─────────────────────────────────────────────────────────────────────────

    @MutationMapping
    
    public WishlistItemDto addToWishlist(
            @Argument AddToWishlistRequest input,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: addToWishlist user={}", userId);
        return wishlistService.addToWishlist(userId, input);
    }

    @MutationMapping
    
    public WishlistItemDto updateWishlistItem(
            @Argument Long productId,
            @Argument UpdateWishlistItemRequest input,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: updateWishlistItem productId={} user={}", productId, userId);
        return wishlistService.updateWishlistItem(userId, productId, input);
    }

    @MutationMapping
    
    public Boolean removeFromWishlist(@Argument Long productId, @ContextValue Long userId) {
        log.info("GraphQL Mutation: removeFromWishlist productId={} user={}", productId, userId);
        wishlistService.removeFromWishlist(userId, productId);
        return true;
    }

    @MutationMapping
    
    public Boolean clearWishlist(@ContextValue Long userId) {
        log.info("GraphQL Mutation: clearWishlist user={}", userId);
        wishlistService.clearWishlist(userId);
        return true;
    }

    @MutationMapping
    
    public WishlistItemDto markWishlistItemPurchased(
            @Argument Long productId,
            @ContextValue Long userId) {
        log.info("GraphQL Mutation: markWishlistItemPurchased productId={} user={}", productId, userId);
        return wishlistService.markAsPurchased(userId, productId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static Pageable buildPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort.Direction dir = input.getDirection() == SortDirection.DESC
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        String field = input.getSortBy() != null ? input.getSortBy() : "createdAt";
        return PageRequest.of(input.getPage(), input.getSize(), Sort.by(dir, field));
    }
}