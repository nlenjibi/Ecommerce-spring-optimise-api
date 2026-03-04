package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity;


public enum PaymentStatus {
    PENDING("Pending", "Payment is pending"),
    AUTHORIZED("Authorized", "Payment has been authorized"),
    PAID("Paid", "Payment completed successfully"),
    FAILED("Failed", "Payment failed"),
    REFUNDED("Refunded", "Payment has been refunded"),
    PARTIALLY_REFUNDED("Partially Refunded", "Payment partially refunded"),
    CANCELLED("Cancelled", "Payment cancelled");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}