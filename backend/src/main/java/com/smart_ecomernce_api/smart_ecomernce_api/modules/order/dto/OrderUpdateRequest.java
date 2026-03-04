package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderUpdateRequest {

    /** Target status for the transition. Routed through the entity state machine in the service. */
    private OrderStatus status;

    /** Shipping carrier tracking number — settable by admin on SHIPPED orders. */
    private String trackingNumber;

    /** Carrier name (e.g. "DHL", "FedEx") — settable by admin on SHIPPED orders. */
    private String carrier;

    /** Internal admin notes — not exposed to customers. */
    private String adminNotes;

    // -------------------------------------------------------------------------
    // Fields added to support updateOrderStatus() state-machine switch
    // -------------------------------------------------------------------------

    /**
     * Reason supplied when cancelling an order via the generic status-update endpoint.
     * Used by the CANCELLED branch of the service's updateOrderStatus() switch.
     * Defaults to "Cancelled by admin" when null.
     */
    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String cancellationReason;

    /**
     * Amount to refund when transitioning to REFUNDED via the generic update endpoint.
     * Defaults to the full order total when null.
     */
    @PositiveOrZero(message = "Refund amount must be zero or positive")
    private BigDecimal refundAmount;

    /**
     * Reason for the refund — required by Order#refund() and surfaced in the
     * order record for customer communication and audit purposes.
     * Defaults to "Refunded by admin" when null.
     */
    @Size(max = 500, message = "Refund reason must not exceed 500 characters")
    private String refundReason;
}