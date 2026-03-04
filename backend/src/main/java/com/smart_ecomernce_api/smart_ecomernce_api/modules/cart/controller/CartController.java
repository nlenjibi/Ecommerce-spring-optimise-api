package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.AddItemToCartRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.CartItemDto;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.dto.UpdateCartItemRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simple Cart REST Controller
 */
@RestController
@RequestMapping("v1/carts")
@RequiredArgsConstructor
@Slf4j

@Tag(name = "Cart Management", description = "Shopping cart operations")
public class CartController {

    private final CartService cartService;

    /**
     * Create a new cart
     * POST /v1/carts
     */
    @Operation(summary = "Create new cart", description = "Create a new shopping cart")
    @PostMapping
    public ResponseEntity<ApiResponse<CartDto>> createCart() {
        try {
            log.info("Creating new cart for current user");
            CartDto cart = cartService.createCart();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<CartDto>builder()
                            .success(true)
                            .message("Cart created successfully")
                            .data(cart)
                            .build());
        } catch (Exception e) {
            log.error("Error creating cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CartDto>builder()
                            .success(false)
                            .message("Failed to create cart: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Add item to cart
     * POST /v1/carts/{cartId}/items
     */
    @Operation(summary = "Add item to cart", description = "Add a product to the shopping cart")
    @PostMapping("/{cartId}/items")
    public ResponseEntity<ApiResponse<CartItemDto>> addToCart(
            @PathVariable Long cartId,
            @Valid @RequestBody AddItemToCartRequest request) {
        try {
            log.info("Adding item to cart {}: product {}", cartId, request.getProductId());
            CartItemDto item = cartService.addToCart(cartId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<CartItemDto>builder()
                            .success(true)
                            .message("Item added to cart successfully")
                            .data(item)
                            .build());
        } catch (Exception e) {
            log.error("Error adding item to cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CartItemDto>builder()
                            .success(false)
                            .message("Failed to add item to cart: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get cart by ID
     * GET /v1/carts/{cartId}
     */
    @Operation(summary = "Get cart", description = "Retrieve shopping cart by ID")
    @GetMapping("/{cartId}")
    public ResponseEntity<ApiResponse<CartDto>> getCart(@PathVariable Long cartId) {
        try {
            log.info("Getting cart {}", cartId);
            CartDto cart = cartService.getCart(cartId);
            return ResponseEntity.ok(ApiResponse.<CartDto>builder()
                    .success(true)
                    .message("Cart retrieved successfully")
                    .data(cart)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CartDto>builder()
                            .success(false)
                            .message("Failed to retrieve cart: " + e.getMessage())
                            .build());
        }
    }

      /**
     * Update cart item quantity
     * PUT /v1/carts/{cartId}/items/{productId}
     */
    @Operation(summary = "Update cart item", description = "Update quantity of an item in the cart")
    @PutMapping("/{cartId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartItemDto>> updateCartItem(
            @PathVariable Long cartId,
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        try {
            log.info("Updating cart {} item {}: quantity {}", cartId, productId, request.getQuantity());
            CartItemDto item = cartService.updateItemQuantity(cartId, productId, request);
            return ResponseEntity.ok(ApiResponse.<CartItemDto>builder()
                    .success(true)
                    .message("Cart item updated successfully")
                    .data(item)
                    .build());
        } catch (Exception e) {
            log.error("Error updating cart item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CartItemDto>builder()
                            .success(false)
                            .message("Failed to update cart item: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Remove item from cart
     * DELETE /v1/carts/{cartId}/items/{productId}
     */
    @Operation(summary = "Remove cart item", description = "Remove an item from the cart")
    @DeleteMapping("/{cartId}/items/{productId}")
    public ResponseEntity<ApiResponse<String>> removeCartItem(
            @PathVariable Long cartId,
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        try {
            log.info("Removing item {} from cart {}", productId, cartId);
            cartService.removeItem(cartId, productId);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Cart item removed successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error removing cart item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<String>builder()
                            .success(false)
                            .message("Failed to remove cart item: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Clear cart
     * DELETE /v1/carts/{cartId}/items
     */
    @Operation(summary = "Clear cart", description = "Remove all items from the cart")
    @DeleteMapping("/{cartId}/items")
    public ResponseEntity<ApiResponse<String>> clearCart(@PathVariable Long cartId) {
        try {
            log.info("Clearing cart {}", cartId);
            cartService.clearCart(cartId);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Cart cleared successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error clearing cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<String>builder()
                            .success(false)
                            .message("Failed to clear cart: " + e.getMessage())
                            .build());
        }
    }

}