package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity;


public enum PaymentMethod {
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    PAYPAL("PayPal"),
    BANK_TRANSFER("Bank Transfer"),
    MOBILE_MONEY("Mobile Money"),
    CASH_ON_DELIVERY("Cash on Delivery"),
    STRIPE("Stripe"),
    RAZORPAY("Razorpay");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}