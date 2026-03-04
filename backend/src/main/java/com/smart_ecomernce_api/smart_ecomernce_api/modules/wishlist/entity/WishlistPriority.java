package com.smart_ecomernce_api.smart_ecomernce_api.modules.wishlist.entity;


public enum WishlistPriority {
    LOW("Low Priority"),
    MEDIUM("Medium Priority"),
    HIGH("High Priority"),
    URGENT("Urgent - Must Buy");

    private final String displayName;

    WishlistPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}