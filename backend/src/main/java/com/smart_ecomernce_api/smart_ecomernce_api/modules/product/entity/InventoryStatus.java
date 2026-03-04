package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity;
/**
 * Product Inventory Status
 * Represents the stock availability status of a product
 */
public enum InventoryStatus {
    IN_STOCK("In Stock", "Product is available"),
    LOW_STOCK("Low Stock", "Product stock is running low"),
    OUT_OF_STOCK("Out of Stock", "Product is currently unavailable"),
    DISCONTINUED("Discontinued", "Product is no longer available"),
    PRE_ORDER("Pre-Order", "Product available for pre-order"),
    BACKORDER("Backorder", "Product temporarily out of stock but can be ordered");

    private final String displayName;
    private final String description;

    InventoryStatus(String displayName, String description) {
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