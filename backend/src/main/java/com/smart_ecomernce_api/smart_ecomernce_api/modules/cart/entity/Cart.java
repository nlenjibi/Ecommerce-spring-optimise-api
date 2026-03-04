package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Enhanced Cart Entity for Modern E-commerce
 * Supports guest carts, user carts, and advanced features
 */
@Entity
@Table(name = "carts", indexes = {
        @Index(name = "idx_cart_status", columnList = "status"),
        @Index(name = "idx_cart_user", columnList = "user_id"),
        @Index(name = "idx_cart_updated", columnList = "updated_at"),
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Cart extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private CartStatus status = CartStatus.ACTIVE;

    /**
     * Cart items
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CartItem> items = new HashSet<>();

    /**
     * Applied coupon code
     */
    private String couponCode;

    /**
     * Applied discount amount
     */
    private BigDecimal discountAmount;

    /**
     * Add item to cart or update quantity if already exists
     */
    public CartItem addItem(Product product) {
        var item = getItem(product.getId());

        if (item != null) {
            item.setQuantity(item.getQuantity() + 1);
            return item;
        }

        CartItem newItem = CartItem.builder()
                .cart(this)
                .product(product)
                .quantity(1)
                .build();
        items.add(newItem);
        return newItem;
    }

    /**
     * Get cart item by product ID
     */
    public CartItem getItem(Long productId) {
        if (items == null) {
            return null;
        }

        return items.stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove item from cart by product ID
     */
    public void removeItem(Long productId) {
        if (items == null) {
            return;
        }

        items.removeIf(i -> i.getProduct().getId().equals(productId));
    }

    /**
     * Clear all items from cart
     */
    public void clear() {
        if (items == null) {
            items = new HashSet<>();
            return;
        }

        items.clear();
    }

    /**
     * Check if cart is empty
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /**
     * Get total price of all items (without discount)
     */
    public BigDecimal getTotalPrice() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total number of items in cart (sum of quantities)
     */
    public int getItemCount() {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Get subtotal (same as total price for now, before discount)
     */
    public BigDecimal getSubtotal() {
        return getTotalPrice();
    }

    /**
     * Get total amount (after discount)
     */
    public BigDecimal getTotalAmount() {
        BigDecimal total = getTotalPrice();
        if (discountAmount != null) {
            total = total.subtract(discountAmount);
        }
        return total.max(BigDecimal.ZERO);
    }



}