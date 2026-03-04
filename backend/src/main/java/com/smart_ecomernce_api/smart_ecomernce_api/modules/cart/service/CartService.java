package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.AddItemToCartRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.UpdateCartItemRequest;

/**
 * Optimized Cart Service Interface for Modern E-commerce
 * Provides comprehensive cart management operations
 */
public interface CartService {
    CartDto getCart(Long cartId);
    CartDto createCart();


    CartItemDto addToCart(Long cartId, AddItemToCartRequest request);

    CartItemDto updateItemQuantity(Long cartId, Long productId, UpdateCartItemRequest request);


    void removeItem(Long cartId, Long productId);

    void clearCart(Long cartId);
}