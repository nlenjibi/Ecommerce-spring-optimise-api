package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Order Item Entity
 *
 * NOTE: EqualsAndHashCode uses only `id` (from BaseEntity) so that
 * multiple unsaved items are NOT collapsed in a List. The Order entity
 * uses List<OrderItem> (not Set) for the same reason — unsaved items
 * all have id == null, which would make every Set.add() a no-op.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_item_order",   columnList = "order_id"),
        @Index(name = "idx_order_item_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// Only include `id` (inherited from BaseEntity) so two NEW (unsaved)
// items with id == null are still treated as distinct objects.
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = false)
public class OrderItem extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Product product;

    /**
     * Store product name at time of order (in case product is deleted/renamed later).
     */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /**
     * Quantity ordered.
     */
    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    /**
     * Unit price at time of order (snapshot — price may change later).
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    /**
     * Discount applied to this line item (flat amount, not percentage).
     */
    @Column(name = "discount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "product_image_url")
    private String productImageUrl;

    /**
     * Persisted total = (unitPrice × quantity) − discount, floored at 0.
     * Recalculated on every PrePersist / PreUpdate so the DB value is
     * always consistent even if quantity or unitPrice is changed directly.
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Total price is required")
    private BigDecimal totalPrice;

    // -----------------------------------------------------------------------
    // Business helpers
    // -----------------------------------------------------------------------

    /**
     * Compute the line-item total without touching the persisted field.
     * Called by {@link Order#calculateTotals()} and by lifecycle hooks.
     */
    public BigDecimal computeTotal() {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal raw = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            raw = raw.subtract(discount);
        }
        return raw.max(BigDecimal.ZERO);
    }

    /**
     * Override getter so that Order#calculateTotals() always gets a fresh
     * computed value (avoids stale persisted value during in-memory mutations).
     */
    @SuppressWarnings("lombok")
    public BigDecimal getTotalPrice() {
        return computeTotal();
    }


    // -----------------------------------------------------------------------
    // JPA lifecycle
    // -----------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        backfillDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        backfillDefaults();
    }

    private void backfillDefaults() {
        if (productName == null && product != null) {
            productName = product.getName();
        }
        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
        // Always keep persisted total in sync
        totalPrice = computeTotal();
    }
}