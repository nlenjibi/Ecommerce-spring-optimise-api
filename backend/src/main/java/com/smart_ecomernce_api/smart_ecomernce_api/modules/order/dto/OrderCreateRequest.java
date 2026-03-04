package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.PaymentMethod;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.ShippingMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreateRequest {

    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemCreateRequest> items;

    @NotNull(message = "Shipping method is required")
    private ShippingMethod shippingMethod;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String couponCode;

    @NotNull(message = "Tax rate is required")
    @PositiveOrZero(message = "Tax rate must be zero or positive")
    private BigDecimal taxRate;

    @Size(max = 255, message = "Customer notes must not exceed 255 characters")
    private String customerNotes;

    @Size(max = 255, message = "Shipping address must not exceed 255 characters")
    private String shippingAddress;

    @PositiveOrZero(message = "Subtotal must be zero or positive")
    private BigDecimal subtotal;

    @PositiveOrZero(message = "Tax amount must be zero or positive")
    private BigDecimal taxAmount;

    @PositiveOrZero(message = "Shipping cost must be zero or positive")
    private BigDecimal shippingCost;

    @PositiveOrZero(message = "Discount amount must be zero or positive")
    private BigDecimal discountAmount;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @PositiveOrZero(message = "Coupon discount must be zero or positive")
    private BigDecimal couponDiscount;

}