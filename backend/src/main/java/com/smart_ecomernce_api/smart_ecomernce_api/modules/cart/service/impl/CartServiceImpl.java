package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.CartNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.InsufficientStockException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.AuthorizationException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.AddItemToCartRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.UpdateCartItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.CartStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.mapper.CartMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository.CartRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.CartService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    @CacheEvict(value = "carts", allEntries = true)
    public CartDto createCart() {
        log.info("Creating new cart for current user");
        Long userId = getCurrentUserId();
        Optional<Cart> existingActiveCartOpt = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE);
        if (existingActiveCartOpt.isPresent()) {
            Cart existingActiveCart = existingActiveCartOpt.get();
            log.info("Active cart already exists for user {}: Cart ID {}", userId, existingActiveCart.getId());
            return cartMapper.toDto(existingActiveCart);
        }
        Cart cart = Cart.builder().userId(userId).status(CartStatus.ACTIVE).build();
        Cart savedCart = cartRepository.save(cart);
        log.info("Cart created with ID: {} for user {}", savedCart.getId(), userId);
        return cartMapper.toDto(savedCart);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#cartId")
    public CartDto getCart(Long cartId) {
        Long userId = getCurrentUserId();
        log.info("Fetching cart: {}", cartId);
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
        if (!cart.getUserId().equals(userId)) {
            throw new CartNotFoundException("Cart not found for user");
        }

        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#cartId")
    public CartItemDto addToCart(Long cartId, AddItemToCartRequest request) {
        Long userId = getCurrentUserId();

        log.info("Adding product {} to cart {}", request.getProductId(), cartId);
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
        if (!cart.getUserId().equals(userId)) {
            throw new CartNotFoundException("Cart not found for user");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));

        CartItem cartItem = cart.addItem(product);
        cartRepository.save(cart);

        log.info("Added product {} to cart {} with quantity {}", request.getProductId(), cartId,
                cartItem.getQuantity());
        return cartMapper.toDto(cartItem);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#cartId")
    public CartItemDto updateItemQuantity(Long cartId, Long productId, UpdateCartItemRequest request) {
        Long userId = getCurrentUserId();

        Integer quantity = request.getQuantity();
        log.info("Updating cart {} item {} quantity to {}", cartId, productId, quantity);
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
        if (!cart.getUserId().equals(userId)) {
            throw new CartNotFoundException("Cart not found for user");
        }

        CartItem cartItem = cart.getItem(productId);
        if (cartItem == null) {
            throw ResourceNotFoundException.forResource("Cart item for product", productId);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity(), quantity);
        }

        cartItem.setQuantity(quantity);
        cartRepository.save(cart);

        log.info("Updated cart item quantity: cart={}, product={}, quantity={}", cartId, productId, quantity);
        return cartMapper.toDto(cartItem);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#cartId")
    public void removeItem(Long cartId, Long productId) {
        Long userId = getCurrentUserId();

        log.info("Removing product {} from cart {}", productId, cartId);
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
        if (!cart.getUserId().equals(userId)) {
            throw new CartNotFoundException("Cart not found for user");
        }
        cart.removeItem(productId);
        cartRepository.save(cart);
        log.info("Removed product {} from cart {}", productId, cartId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#cartId")
    public void clearCart(Long cartId) {
        Long userId = getCurrentUserId();
        log.info("Clearing cart: {}", cartId);
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
        if (!cart.getUserId().equals(userId)) {
            throw new CartNotFoundException("Cart not found for user");
        }
        if (cart.isEmpty()) {
            throw new CartNotFoundException("Cart is already empty");
        }
        cart.clear();
        cartRepository.save(cart);
        log.info("Cart cleared: {}", cartId);
    }

    @Transactional(readOnly = true)
    public List<CartDto> getAbandonedCarts(int hours) {
        log.info("Fetching abandoned carts older than {} hours", hours);
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(hours);
        List<Cart> abandonedCarts = cartRepository.findAbandonedCartsBefore(cutoffDate);
        log.info("Found {} abandoned carts", abandonedCarts.size());
        return cartMapper.toDtoList(abandonedCarts);
    }

    private static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthorizationException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        // Handle custom UserPrincipal (our implementation)
        if (principal instanceof com.smart_ecomernce_api.smart_ecomernce_api.security.UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        // Handle custom User entity
        if (principal instanceof com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User user) {
            return user.getId();
        }
        // Handle Spring Security UserDetails
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            // Try to get user ID from details if available
            if (principal.getClass().getSimpleName().equals("UserPrincipal")) {
                try {
                    var getIdMethod = principal.getClass().getMethod("getId");
                    return (Long) getIdMethod.invoke(principal);
                } catch (Exception ignored) {
                }
            }
            try {
                return Long.valueOf(userDetails.getUsername());
            } catch (NumberFormatException e) {
                // Try to extract from authorities or details if possible
            }
        }
        // Handle JWT principal (e.g., org.springframework.security.oauth2.jwt.Jwt)
        if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            Object userIdClaim = jwt.getClaim("user_id");
            if (userIdClaim != null) {
                try {
                    return Long.valueOf(userIdClaim.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            Object subClaim = jwt.getClaim("sub");
            if (subClaim != null) {
                try {
                    return Long.valueOf(subClaim.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        // Handle OAuth2User principal
        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
            Object userIdAttr = oAuth2User.getAttribute("id");
            if (userIdAttr != null) {
                try {
                    return Long.valueOf(userIdAttr.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            Object subAttr = oAuth2User.getAttribute("sub");
            if (subAttr != null) {
                try {
                    return Long.valueOf(subAttr.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        throw new AuthorizationException("Unknown user principal type: " + principal.getClass().getName());
    }
}

