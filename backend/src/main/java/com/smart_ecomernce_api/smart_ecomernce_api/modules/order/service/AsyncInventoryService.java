package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;

/**
 * Service interface for async inventory operations triggered by order status changes.
 */
public interface AsyncInventoryService {

    /**
     * Update inventory when an order is confirmed/processed - reduces stock
     */
    void updateInventoryOnOrderConfirm(Long orderId);

    /**
     * Update inventory when an order is cancelled - restores stock
     */
    void updateInventoryOnOrderCancel(Long orderId);

    /**
     * Update inventory when an order is delivered - finalizes stock
     */
    void updateInventoryOnOrderDeliver(Long orderId);
}
