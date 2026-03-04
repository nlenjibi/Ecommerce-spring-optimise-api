package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.AddToWishlistRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.UpdateWishlistItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.dto.WishlistSummaryDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.entity.WishlistItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.entity.WishlistPriority;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.mapper.WishlistMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.repository.WishlistRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

    // ═══════════════════════════════════════════════════════════════
    //  Basic CRUD
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    @CacheEvict(value = {
            "wishlists", "wishlists-paginated", "wishlists-summary", "wishlists-check"
    }, allEntries = true)
    public WishlistItemDto addToWishlist(Long userId, AddToWishlistRequest request) {
        log.debug("addToWishlist: userId={}, productId={}", userId, request.getProductId());

        if (wishlistRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            return wishlistRepository
                    .findByUserIdAndProductId(userId, request.getProductId())
                    .map(wishlistMapper::toDto)
                    .orElseThrow();
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", userId));
        var product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .priority(request.getPriority() != null ? request.getPriority() : WishlistPriority.MEDIUM)
                .notes(request.getNotes())
                .desiredQuantity(request.getDesiredQuantity() != null ? request.getDesiredQuantity() : 1)
                .notifyOnPriceDrop(Boolean.TRUE.equals(request.getNotifyOnPriceDrop()))
                .notifyOnStock(Boolean.TRUE.equals(request.getNotifyOnStock()))
                .targetPrice(request.getTargetPrice())
                .collectionName(request.getCollectionName())
                .build();

        return wishlistMapper.toDto(wishlistRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists", key = "#userId")
    public List<WishlistItemDto> getUserWishlist(Long userId) {
        log.debug("getUserWishlist: userId={}", userId);
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(wishlistMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists-paginated", key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<WishlistItemDto> getUserWishlistPaginated(Long userId, Pageable pageable) {
        log.debug("getUserWishlistPaginated: userId={}, page={}", userId, pageable.getPageNumber());
        return wishlistRepository.findByUserId(userId, pageable).map(wishlistMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists-summary", key = "#userId")
    public WishlistSummaryDto getWishlistSummary(Long userId) {
        log.debug("getWishlistSummary: userId={}", userId);

        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Object[] totals = wishlistRepository.findTotalValueAndSavings(userId);
        BigDecimal totalValue   = totals[0] != null ? (BigDecimal) totals[0] : BigDecimal.ZERO;
        BigDecimal totalSavings = totals[1] != null ? (BigDecimal) totals[1] : BigDecimal.ZERO;

        long inStock    = items.stream().filter(i -> i.getProduct().isInStock()).count();
        long priceDrops = items.stream().filter(WishlistItem::isPriceDropped).count();
        long purchased  = items.stream().filter(i -> Boolean.TRUE.equals(i.getPurchased())).count();

        return WishlistSummaryDto.builder()
                .userId(userId)
                .totalItems(items.size())
                .inStockItems((int) inStock)
                .outOfStockItems((int) (items.size() - inStock))
                .itemsWithPriceDrops((int) priceDrops)
                .purchasedItems((int) purchased)
                .totalValue(totalValue)
                .totalSavings(totalSavings)
                .items(items.stream().map(wishlistMapper::toDto).toList())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "wishlists", "wishlists-paginated", "wishlists-summary", "wishlists-check"
    }, allEntries = true)
    public void removeFromWishlist(Long userId, Long productId) {
        log.debug("removeFromWishlist: userId={}, productId={}", userId, productId);
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("WishlistItem", productId));
        wishlistRepository.delete(item);
    }

    @Override
    @Transactional
    @CachePut(value = "wishlists", key = "#userId")
    @CacheEvict(value = {
            "wishlists-paginated", "wishlists-summary", "wishlists-check"
    }, allEntries = true)
    public WishlistItemDto updateWishlistItem(Long userId, Long productId, UpdateWishlistItemRequest request) {
        log.debug("updateWishlistItem: userId={}, productId={}", userId, productId);
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("WishlistItem", productId));

        if (request.getNotes()             != null) item.setNotes(request.getNotes());
        if (request.getPriority()          != null) item.setPriority(request.getPriority());
        if (request.getDesiredQuantity()   != null) item.setDesiredQuantity(request.getDesiredQuantity());
        if (request.getTargetPrice()       != null) item.setTargetPrice(request.getTargetPrice());
        if (request.getNotifyOnPriceDrop() != null) item.setNotifyOnPriceDrop(request.getNotifyOnPriceDrop());
        if (request.getNotifyOnStock()     != null) item.setNotifyOnStock(request.getNotifyOnStock());
        if (request.getIsPublic()          != null) item.setIsPublic(request.getIsPublic());

        return wishlistMapper.toDto(wishlistRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists-check", key = "#userId + ':' + #productId")
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "wishlists", "wishlists-paginated", "wishlists-summary",
            "wishlists-check", "wishlists-drops", "wishlists-analytics"
    }, allEntries = true)
    public void clearWishlist(Long userId) {
        log.debug("clearWishlist: userId={}", userId);
        int deleted = wishlistRepository.deleteByUserId(userId);
        log.info("clearWishlist: removed {} items for userId={}", deleted, userId);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Price & stock tracking
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wishlists-drops", key = "#userId")
    public List<WishlistItemDto> getItemsWithPriceDrops(Long userId) {
        return wishlistRepository.findItemsWithPriceDrops(userId)
                .stream().map(wishlistMapper::toDto).toList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Purchase & cart
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    @CachePut(value = "wishlists", key = "#userId")
    @CacheEvict(value = {"wishlists-summary", "wishlists-analytics"}, allEntries = true)
    public WishlistItemDto markAsPurchased(Long userId, Long productId) {
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("WishlistItem", productId));
        item.markAsPurchased();
        return wishlistMapper.toDto(wishlistRepository.save(item));
    }

    @Override
    @Transactional
    public void moveToCart(Long userId, Long productId) {
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("WishlistItem", productId));
        log.debug("moveToCart: productId={} for userId={}", productId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }
}