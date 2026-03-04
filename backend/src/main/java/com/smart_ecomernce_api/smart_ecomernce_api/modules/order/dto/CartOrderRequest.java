package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.PaymentMethod;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.ShippingMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Optional request body for the "create order from cart" (checkout) endpoint.
 *
 * <p>All fields are optional. The core order data — items, quantities, prices,
 * and coupon details — is sourced directly from the cart via
 * {@link com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order#fromCart}.
 * This DTO only carries the shipping / payment metadata that the client wants
 * to attach on top.
 *
 * <p>If the request body is omitted entirely (the client sends no JSON), the
 * service treats it as {@code null} and skips all optional field mapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartOrderRequest {

    /**
     * Full delivery address as a free-text string.
     * Example: "12 Independence Ave, Accra, Ghana"
     */
    private String shippingAddress;

    /**
     * Chosen shipping tier. Maps to the {@link ShippingMethod} enum.
     * Defaults to whatever the system standard is when null.
     */
    private ShippingMethod shippingMethod;

    /**
     * Flat shipping cost chosen / quoted at checkout.
     * When provided and > 0 it overrides the default shipping cost
     * and triggers a recalculation of order totals.
     */
    private BigDecimal shippingCost;

    /**
     * How the customer intends to pay. Maps to the {@link PaymentMethod} enum.
     */
    private PaymentMethod paymentMethod;

    /**
     * Free-text delivery instructions from the customer.
     * Example: "Ring doorbell twice, leave at gate if no answer."
     */
    private String customerNotes;
}