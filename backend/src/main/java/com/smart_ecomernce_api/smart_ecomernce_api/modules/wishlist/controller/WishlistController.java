package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist Management", description = "Wishlist management for authenticated users")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<WishlistItemDto>> addToWishlist(
            @Valid @RequestBody AddToWishlistRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        WishlistItemDto item = wishlistService.addToWishlist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist", item));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user wishlist")
    public ResponseEntity<ApiResponse<List<WishlistItemDto>>> getWishlist(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        List<WishlistItemDto> items = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved", items));
    }

    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get paginated wishlist")
    public ResponseEntity<ApiResponse<Page<WishlistItemDto>>> getWishlistPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Long userId = getCurrentUserId(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<WishlistItemDto> items = wishlistService.getUserWishlistPaginated(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Wishlist page retrieved", items));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist", null));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clear entire wishlist")
    public ResponseEntity<ApiResponse<Void>> clearWishlist(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        wishlistService.clearWishlist(userId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist cleared", null));
    }

    @GetMapping("/check/{productId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        boolean isInWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist check", isInWishlist));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get wishlist count")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        long count = wishlistService.getWishlistCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist count", count));
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails instanceof com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User user) {
            return user.getId();
        }
        if (userDetails instanceof com.smart_ecomernce_api.smart_ecomernce_api.security.UserPrincipal principal) {
            return principal.getId();
        }
        throw new com.smart_ecomernce_api.smart_ecomernce_api.exception.AuthorizationException(
                "Authenticated user not found"
        );
    }
}
