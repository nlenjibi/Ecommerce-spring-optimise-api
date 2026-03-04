package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.UpdateWishlistItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WishlistService {

    // ──────────────────────────────────────────────────────────────
    //  Basic CRUD
    // ──────────────────────────────────────────────────────────────

    WishlistItemDto addToWishlist(Long userId, AddToWishlistRequest request);

    List<WishlistItemDto> getUserWishlist(Long userId);

    Page<WishlistItemDto> getUserWishlistPaginated(Long userId, Pageable pageable);

    WishlistSummaryDto getWishlistSummary(Long userId);

    void removeFromWishlist(Long userId, Long productId);

    WishlistItemDto updateWishlistItem(Long userId, Long productId, UpdateWishlistItemRequest request);

    boolean isInWishlist(Long userId, Long productId);

    void clearWishlist(Long userId);

    // ──────────────────────────────────────────────────────────────
    //  Price & stock tracking
    // ──────────────────────────────────────────────────────────────

    List<WishlistItemDto> getItemsWithPriceDrops(Long userId);



    WishlistItemDto markAsPurchased(Long userId, Long productId);

    void moveToCart(Long userId, Long productId);



    long getWishlistCount(Long userId);
}