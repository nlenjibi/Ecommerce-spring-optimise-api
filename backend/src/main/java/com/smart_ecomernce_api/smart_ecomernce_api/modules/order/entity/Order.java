package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Order Entity — represents a customer order created from a cart.
 *
 * KEY FIX: orderItems is now a {@code List<OrderItem>} (ArrayList) instead of
 * {@code Set<OrderItem>}.  Using a Set caused silent item-loss because
 * unsaved OrderItem instances all have {@code id == null}, making them
 * "equal" under any id-based equals/hashCode — so Set.add() was a no-op
 * for every item after the first.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number",      columnList = "order_number"),
        @Index(name = "idx_order_user",        columnList = "user_id"),
        @Index(name = "idx_order_status",      columnList = "status"),
        @Index(name = "idx_order_payment_status", columnList = "payment_status"),
        @Index(name = "idx_order_created",     columnList = "created_at"),
        @Index(name = "idx_order_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Order extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @NotNull(message = "Order status is required")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @CreationTimestamp
    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    /**
     * Using List (not Set) so that multiple NEW (unsaved) items — which all
     * have id == null — are stored without collapsing.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "coupon_discount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_transaction_id", length = 100)
    private String paymentTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method", length = 30)
    private ShippingMethod shippingMethod;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "carrier", length = 50)
    private String carrier;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    // ========================================================================
    // FACTORY METHOD
    // ========================================================================

    public static Order fromCart(Cart cart, User customer) {
        if (cart == null)     throw new IllegalArgumentException("Cart cannot be null");
        if (cart.isEmpty())   throw new IllegalStateException("Cannot create order from empty cart");
        if (customer == null) throw new IllegalArgumentException("Customer cannot be null");

        Order order = Order.builder()
                .user(customer)
                .customerEmail(customer.getEmail())
                .customerName(customer.getFullName())
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .orderNumber(generateOrderNumber())
                .build();

        cart.getItems().forEach(cartItem -> {
            OrderItem orderItem = OrderItem.builder()
                    .product(cartItem.getProduct())
                    .productName(cartItem.getProduct().getName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getTotalPrice())
                    .build();
            order.addOrderItem(orderItem);
        });

        if (cart.getCouponCode() != null) {
            order.setCouponCode(cart.getCouponCode());
            order.setCouponDiscount(cart.getDiscountAmount());
        }

        order.calculateTotals();
        return order;
    }

    /** Format: ORD-YYYYMMDD-XXXXXX */
    private static String generateOrderNumber() {
        String date   = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        String random = String.format("%06d", new Random().nextInt(999999));
        return "ORD-" + date + "-" + random;
    }

    // ========================================================================
    // BUSINESS LOGIC
    // ========================================================================

    /**
     * Add an item to this order.
     * The item's {@code order} back-reference is set here so callers
     * don't have to remember to do it manually.
     */
    public void addOrderItem(OrderItem item) {
        if (item == null) throw new IllegalArgumentException("Order item cannot be null");
        orderItems.add(item);
        item.setOrder(this);
    }

    /**
     * Remove an item from this order and clear its back-reference.
     */
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    /**
     * Recompute subtotal → tax → total from the current item list.
     * Must be called after any mutation to items, quantities, or prices.
     */
    public void calculateTotals() {
        this.subtotal = orderItems.stream()
                .map(OrderItem::getTotalPrice)          // uses OrderItem#computeTotal()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = subtotal
                    .multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }

        this.totalAmount = subtotal
                .add(taxAmount      != null ? taxAmount      : BigDecimal.ZERO)
                .add(shippingCost   != null ? shippingCost   : BigDecimal.ZERO)
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .subtract(couponDiscount != null ? couponDiscount : BigDecimal.ZERO);

        if (this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    public void applyCoupon(String code, BigDecimal discount) {
        this.couponCode    = code;
        this.couponDiscount = discount;
        calculateTotals();
    }

    public void applyTax(BigDecimal rate) {
        this.taxRate = rate;
        calculateTotals();
    }

    public void applyShippingCost(BigDecimal cost) {
        this.shippingCost = cost;
        calculateTotals();
    }

    // ── Status transitions ──────────────────────────────────────────────────

    public void confirm() {
        if (this.status != OrderStatus.PENDING)
            throw new IllegalStateException("Only pending orders can be confirmed");
        this.status = OrderStatus.CONFIRMED;
    }

    public void process() {
        if (this.status != OrderStatus.CONFIRMED)
            throw new IllegalStateException("Only confirmed orders can be processed");
        this.status = OrderStatus.PROCESSING;
    }

    public void ship() {
        if (this.status != OrderStatus.PROCESSING)
            throw new IllegalStateException("Only processing orders can be shipped");
        this.status    = OrderStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }

    public void outForDelivery() {
        if (this.status != OrderStatus.SHIPPED)
            throw new IllegalStateException("Only shipped orders can be out for delivery");
        this.status = OrderStatus.OUT_FOR_DELIVERY;
    }

    public void deliver() {
        if (this.status != OrderStatus.OUT_FOR_DELIVERY && this.status != OrderStatus.SHIPPED)
            throw new IllegalStateException("Cannot deliver order in current status");
        this.status      = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (!canBeCancelled())
            throw new IllegalStateException("Cannot cancel order in current status");
        this.status              = OrderStatus.CANCELLED;
        this.cancelledAt         = LocalDateTime.now();
        this.cancellationReason  = reason;
    }

    public void refund(BigDecimal amount, String reason) {
        if (!canBeRefunded())
            throw new IllegalStateException("Order cannot be refunded");
        this.status        = OrderStatus.REFUNDED;
        this.paymentStatus = amount.compareTo(totalAmount) < 0
                ? PaymentStatus.PARTIALLY_REFUNDED
                : PaymentStatus.REFUNDED;
        this.refundedAt    = LocalDateTime.now();
        this.refundAmount  = amount;
        this.refundReason  = reason;
    }

    public void markAsPaid(String transactionId) {
        this.paymentStatus        = PaymentStatus.PAID;
        this.paymentTransactionId = transactionId;
        this.paidAt               = LocalDateTime.now();
    }

    public void markPaymentFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
        this.status        = OrderStatus.FAILED;
    }

    // ── Guard predicates ────────────────────────────────────────────────────

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING   ||
                status == OrderStatus.CONFIRMED ||
                status == OrderStatus.PROCESSING;
    }

    public boolean canBeRefunded() {
        return (status == OrderStatus.DELIVERED || status == OrderStatus.SHIPPED)
                && paymentStatus == PaymentStatus.PAID;
    }

    public boolean isPlacedBy(User customer) {
        return customer != null && this.user != null
                && this.user.getId().equals(customer.getId());
    }

    public boolean isCompleted()  { return status == OrderStatus.DELIVERED; }

    public boolean isActiveBasedOnStatus() {
        return status != OrderStatus.CANCELLED &&
                status != OrderStatus.REFUNDED  &&
                status != OrderStatus.FAILED;
    }

    public boolean isPaid() { return paymentStatus == PaymentStatus.PAID; }

    // ── Convenience ─────────────────────────────────────────────────────────

    /** Total number of units across all line items. */
    public int getItemCount() {
        return orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    /** Number of distinct products. */
    public int getUniqueProductCount() { return orderItems.size(); }

    /** Find a line item by product ID, or null if not present. */
    public OrderItem findItemByProductId(Long productId) {
        return orderItems.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    // ========================================================================
    // JPA LIFECYCLE
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (orderNumber == null || orderNumber.isEmpty()) {
            orderNumber = generateOrderNumber();
        }
        calculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateTotals();
    }
}