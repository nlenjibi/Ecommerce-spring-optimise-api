package com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity;

/**
 * Cart Status Enum
 */
public enum CartStatus {
    ACTIVE,      // Cart is being actively used
    ABANDONED,   // User left without completing purchase
    CONVERTED,   // Cart converted to order
    EXPIRED      // Cart expired due to inactivity
}