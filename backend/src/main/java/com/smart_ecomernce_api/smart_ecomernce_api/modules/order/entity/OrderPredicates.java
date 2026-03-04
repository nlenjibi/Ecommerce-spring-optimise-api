package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.OrderFilterInput;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * QueryDSL Predicate builder for Order entity.
 *
 * <p>Uses {@link Expressions} path helpers so no APT/Q-type generation is needed.
 * Every path string matches the corresponding field name on the {@code Order} entity.
 *
 * <pre>
 * // Fluent builder usage
 * Predicate p = OrderPredicates.builder()
 *     .withStatus(OrderStatus.PENDING)
 *     .withCreatedAfter(LocalDateTime.now().minusDays(7))
 *     .withMinTotalAmount(new BigDecimal("100.00"))
 *     .buildActiveOnly();
 *
 * // One-shot from filter DTO
 * Predicate p = OrderPredicates.from(filterInput);
 *
 * orderRepository.findAll(p, pageable);
 * </pre>
 */
public final class OrderPredicates {

    // -----------------------------------------------------------------
    // Entity field path constants — change once here if entity changes
    // -----------------------------------------------------------------
    private static final String F_STATUS           = "status";
    private static final String F_PAYMENT_STATUS   = "paymentStatus";
    private static final String F_USER_ID          = "user.id";
    private static final String F_USER_EMAIL       = "user.email";
    private static final String F_ORDER_NUMBER     = "orderNumber";
    private static final String F_TOTAL_AMOUNT     = "totalAmount";
    private static final String F_CREATED_AT       = "createdAt";
    private static final String F_IS_ACTIVE        = "isActive";
    private final BooleanBuilder bb;

    private OrderPredicates() {
        this.bb = new BooleanBuilder();
    }

    // -----------------------------------------------------------------
    // Factory methods
    // -----------------------------------------------------------------

    /** Create a new fluent builder. */
    public static OrderPredicates builder() {
        return new OrderPredicates();
    }

    /**
     * Build a {@link Predicate} directly from an {@link OrderFilterInput} DTO.
     * Every non-null field is applied as an AND condition.
     * Defaults to active-only when {@code filter.getIsActive()} is null.
     */
    public static Predicate from(OrderFilterInput filter) {
        OrderPredicates p = builder();

        if (filter == null) {
            return p.withActive(null).build();
        }

        return p
                .withActive(filter.getIsActive())
                .withStatus(parseStatus(filter.getStatus()))
                .withUserId(filter.getUserId())
                .withMinTotalAmount(filter.getMinAmount())
                .withMaxTotalAmount(filter.getMaxAmount())
                .withCreatedAfter(filter.getStartDate())
                .withCreatedBefore(filter.getEndDate())
                .withOrderNumberContaining(filter.getOrderNumber())
                .withCustomerEmailContaining(filter.getCustomerEmail())
                .applyIf(Boolean.TRUE.equals(filter.getHighValue()),
                        b -> b.withHighValue(null))
                .applyIf(Boolean.TRUE.equals(filter.getOverdue()),
                        b -> b.withOverdueBefore(LocalDateTime.now().minusDays(3)))
                .build();
    }

    // -----------------------------------------------------------------
    // Filter methods
    // -----------------------------------------------------------------

    /** Filter by {@link OrderStatus}. */
    public OrderPredicates withStatus(OrderStatus status) {
        if (status != null) {
            bb.and(Expressions.enumPath(OrderStatus.class, F_STATUS).eq(status));
        }
        return this;
    }

    /** Filter by {@link PaymentStatus}. */
    public OrderPredicates withPaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus != null) {
            bb.and(Expressions.enumPath(PaymentStatus.class, F_PAYMENT_STATUS).eq(paymentStatus));
        }
        return this;
    }

    /** Filter by the owning user's ID. */
    public OrderPredicates withUserId(Long userId) {
        if (userId != null) {
            bb.and(Expressions.numberPath(Long.class, F_USER_ID).eq(userId));
        }
        return this;
    }

    /** Case-insensitive contains on {@code orderNumber}. */
    public OrderPredicates withOrderNumberContaining(String orderNumber) {
        if (hasText(orderNumber)) {
            bb.and(Expressions.stringPath(F_ORDER_NUMBER).containsIgnoreCase(orderNumber));
        }
        return this;
    }

    /** Filter on {@code totalAmount >= minAmount}. */
    public OrderPredicates withMinTotalAmount(BigDecimal minAmount) {
        if (minAmount != null) {
            bb.and(Expressions.numberPath(BigDecimal.class, F_TOTAL_AMOUNT).goe(minAmount));
        }
        return this;
    }

    /** Filter on {@code totalAmount <= maxAmount}. */
    public OrderPredicates withMaxTotalAmount(BigDecimal maxAmount) {
        if (maxAmount != null) {
            bb.and(Expressions.numberPath(BigDecimal.class, F_TOTAL_AMOUNT).loe(maxAmount));
        }
        return this;
    }


    /** Filter on {@code createdAt >= date}. */
    public OrderPredicates withCreatedAfter(LocalDateTime date) {
        if (date != null) {
            bb.and(Expressions.dateTimePath(LocalDateTime.class, F_CREATED_AT).goe(date));
        }
        return this;
    }

    /** Filter on {@code createdAt <= date}. */
    public OrderPredicates withCreatedBefore(LocalDateTime date) {
        if (date != null) {
            bb.and(Expressions.dateTimePath(LocalDateTime.class, F_CREATED_AT).loe(date));
        }
        return this;
    }



    /** Case-insensitive contains on {@code user.email}. */
    public OrderPredicates withCustomerEmailContaining(String email) {
        if (hasText(email)) {
            bb.and(Expressions.stringPath(F_USER_EMAIL).containsIgnoreCase(email));
        }
        return this;
    }


    /**
     * High-value orders: {@code totalAmount >= threshold}.
     * Defaults to 500.00 when {@code threshold} is null.
     */
    public OrderPredicates withHighValue(BigDecimal threshold) {
        BigDecimal limit = (threshold != null) ? threshold : new BigDecimal("500.00");
        bb.and(Expressions.numberPath(BigDecimal.class, F_TOTAL_AMOUNT).goe(limit));
        return this;
    }


    /**
     * Overdue orders: status is PENDING or PROCESSING, created before {@code cutoffDate}.
     */
    public OrderPredicates withOverdueBefore(LocalDateTime cutoffDate) {
        if (cutoffDate != null) {
            bb.and(
                    Expressions.dateTimePath(LocalDateTime.class, F_CREATED_AT).loe(cutoffDate)
                            .and(Expressions.enumPath(OrderStatus.class, F_STATUS)
                                    .in(OrderStatus.PENDING, OrderStatus.PROCESSING))
            );
        }
        return this;
    }


    /**
     * Filter by {@code isActive}.
     * When {@code active} is null defaults to {@code true} (active-only).
     */
    public OrderPredicates withActive(Boolean active) {
        bb.and(Expressions.booleanPath(F_IS_ACTIVE).eq(active != null ? active : Boolean.TRUE));
        return this;
    }

    /**
     * Global keyword search: matches {@code orderNumber} OR {@code user.email} OR {@code customerName}
     */
    public OrderPredicates withSearch(String keyword) {
        if (hasText(keyword)) {
            bb.and(
                    Expressions.stringPath(F_ORDER_NUMBER).containsIgnoreCase(keyword)
                            .or(Expressions.stringPath(F_USER_EMAIL).containsIgnoreCase(keyword))
                            .or(Expressions.stringPath("customerName").containsIgnoreCase(keyword))
            );
        }
        return this;
    }

    /**
     * Filter orders with non-null tracking number
     */
    public OrderPredicates withTrackingNumber() {
        bb.and(Expressions.stringPath("trackingNumber").isNotNull());
        return this;
    }


    /**
     * Filter orders that have been paid
     */
    public OrderPredicates withPaidOrders() {
        // Use PaymentStatus.PAID only, since PaymentStatus.COMPLETED does not exist
        bb.and(Expressions.enumPath(PaymentStatus.class, F_PAYMENT_STATUS)
                .in(PaymentStatus.PAID));
        return this;
    }

    /**
     * Filter orders that need attention (PENDING, PROCESSING, PAYMENT_PENDING)
     */
    public OrderPredicates withOrdersNeedingAttention() {
        bb.and(Expressions.enumPath(OrderStatus.class, F_STATUS)
                .in(OrderStatus.PENDING, OrderStatus.PROCESSING));
        return this;
    }

    /**
     * Filter orders that are completed (DELIVERED)
     */
    public OrderPredicates withCompletedOrders() {
        bb.and(Expressions.enumPath(OrderStatus.class, F_STATUS).eq(OrderStatus.DELIVERED));
        return this;
    }

    // -----------------------------------------------------------------
    // Conditional helper
    // -----------------------------------------------------------------

    /**
     * Applies {@code action} to this builder only when {@code condition} is true.
     * Keeps call-chains readable without if-blocks at the call site.
     *
     * <pre>
     * OrderPredicates.builder()
     *     .withActive(true)
     *     .applyIf(isAdmin, b -> b.withHighValue(null))
     *     .build();
     * </pre>
     */
    public OrderPredicates applyIf(boolean condition, java.util.function.Consumer<OrderPredicates> action) {
        if (condition) action.accept(this);
        return this;
    }

    // -----------------------------------------------------------------
    // Terminal methods
    // -----------------------------------------------------------------

    /**
     * Build the predicate from all accumulated conditions.
     * Does NOT implicitly add an active filter.
     */
    public Predicate build() {
        return bb.getValue();
    }

    /**
     * Build and also restrict to {@code isActive = true}.
     * Convenience shortcut so callers don't need to call
     * {@link #withActive(Boolean)} separately.
     */
    public Predicate buildActiveOnly() {
        bb.and(Expressions.booleanPath(F_IS_ACTIVE).isTrue());
        return bb.getValue();
    }

    // -----------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Safely parse a raw status string from a GraphQL/REST input.
     * Returns null (and skips the filter) if the value is blank or unrecognised.
     */
    private static OrderStatus parseStatus(String raw) {
        if (!hasText(raw)) return null;
        try {
            return OrderStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
