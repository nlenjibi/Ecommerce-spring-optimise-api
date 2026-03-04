package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity;


public enum ShippingMethod {
    STANDARD("Standard Shipping", 5.99, 5, 7),
    EXPRESS("Express Shipping", 12.99, 2, 3),
    OVERNIGHT("Overnight Shipping", 24.99, 1, 1),
    FREE_SHIPPING("Free Shipping", 0.00, 7, 10),
    PICKUP("Store Pickup", 0.00, 0, 0);

    private final String displayName;
    private final double cost;
    private final int minDays;
    private final int maxDays;

    ShippingMethod(String displayName, double cost, int minDays, int maxDays) {
        this.displayName = displayName;
        this.cost = cost;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getCost() {
        return cost;
    }

    public int getMinDays() {
        return minDays;
    }

    public int getMaxDays() {
        return maxDays;
    }
}
