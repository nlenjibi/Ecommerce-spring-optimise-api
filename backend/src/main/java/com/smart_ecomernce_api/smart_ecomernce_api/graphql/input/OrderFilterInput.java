package com.smart_ecomernce_api.smart_ecomernce_api.graphql.input;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shared filter DTO used by both the GraphQL {@code filteredOrders} query
 * and the REST {@code /admin/search} endpoint.
 *
 * <p>All fields are optional — only non-null fields are applied as predicates.
 */
@Data
public class OrderFilterInput {
    /** Filter by order status name (e.g. "PENDING"). Case-insensitive. */
    private String status;

    /** Filter by the owning user's ID. */
    private Long userId;

    /** Minimum totalAmount (inclusive). */
    private BigDecimal minAmount;

    /** Maximum totalAmount (inclusive). */
    private BigDecimal maxAmount;

    /** Filter orders created on or after this date-time. */
    private LocalDateTime startDate;

    /** Filter orders created on or before this date-time. */
    private LocalDateTime endDate;

    /** Case-insensitive substring match on orderNumber. */
    private String orderNumber;

    /** Case-insensitive substring match on user email. */
    private String customerEmail;

    /** When true, restricts to high-value orders (totalAmount >= 500.00). */
    private Boolean highValue;

    /** When true, restricts to overdue orders (PENDING/PROCESSING, older than 3 days). */
    private Boolean overdue;

    /** Defaults to true — set to false to include soft-deleted orders. */
    private Boolean isActive = true;
}