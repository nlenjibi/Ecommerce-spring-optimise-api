package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.OrderFilterInput;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.CartOrderRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderStatsResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Service interface for order management operations.
 * Provides comprehensive CRUD operations, status transitions, and advanced querying capabilities.
 */
public interface OrderService {

        // -------------------------------------------------------------------------
        // Create Operations
        // -------------------------------------------------------------------------


        /**
         * Checkout flow: converts an existing cart into a persisted order using
         * {@link com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order#fromCart}.
         *
         * <p>Items, quantities, prices, and coupon data are sourced directly from the
         * cart.  The optional {@link CartOrderRequest} carries shipping and payment
         * metadata; it may be {@code null} if the client sends no body.
         *
         * @param cartId  the cart to convert
         * @param userId  the authenticated customer — must own the cart
         * @param request optional shipping / payment details
         */
        OrderResponse createOrderFromCart(Long cartId, Long userId, CartOrderRequest request);

        // -------------------------------------------------------------------------
        // Read Operations - Single Entity
        // -------------------------------------------------------------------------


        OrderResponse getOrderById(Long id, Long userId);


        OrderResponse getOrderByIdAsAdmin(Long id);


        OrderResponse getOrderByOrderNumber(String orderNumber, Long userId);


        Page<OrderResponse> getUserOrders(Long userId, Pageable pageable);

        Page<OrderResponse> getUserOrdersByStatus(Long userId, OrderStatus status, Pageable pageable);

        Page<OrderResponse> getAllOrders(Pageable pageable);


        Page<OrderResponse> getAllOrders(OrderStatus status, PaymentStatus paymentStatus, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable);


        Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);


        Page<OrderResponse> getFilteredOrders(OrderFilterInput filter, Pageable pageable);

        // -------------------------------------------------------------------------
        // Update Operations
        // -------------------------------------------------------------------------


        OrderResponse updateOrderStatus(Long id, OrderUpdateRequest request);

        OrderResponse updatePaymentStatus(Long orderId, String status);


        OrderResponse updateOrderAsCustomer(Long id, OrderUpdateRequest request, Long userId);

        // -------------------------------------------------------------------------
        // Order Item Operations (Edit Pending Orders)
        // -------------------------------------------------------------------------

        OrderResponse addItemToOrder(Long orderId, Long productId, Integer quantity, Long userId);

        OrderResponse removeItemFromOrder(Long orderId, Long productId, Long userId);

        OrderResponse updateItemQuantity(Long orderId, Long productId, Integer quantity, Long userId);

        // -------------------------------------------------------------------------
        // Status Transition Operations
        // -------------------------------------------------------------------------

        OrderResponse confirmOrder(Long id);

        /**
         * Transitions a CONFIRMED order → PROCESSING.
         * Delegates to {@link com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order#process()}.
         */
        OrderResponse processOrder(Long id);

        OrderResponse shipOrder(Long id, String trackingNumber, String carrier);

        /**
         * Transitions a SHIPPED order → OUT_FOR_DELIVERY.
         * Delegates to {@link com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order#outForDelivery()}.
         */
        OrderResponse outForDeliveryOrder(Long id);

        OrderResponse deliverOrder(Long id);

        OrderResponse cancelOrder(Long id, String reason, Long userId);

        OrderResponse refundOrder(Long id, BigDecimal amount, String reason);

        // -------------------------------------------------------------------------
        // Delete Operations
        // -------------------------------------------------------------------------

        void deleteOrder(Long orderId);

        // -------------------------------------------------------------------------
        // Statistics Operations
        // -------------------------------------------------------------------------

        OrderStatsResponse getOrderStatistics();

        // -------------------------------------------------------------------------
        // Advanced Query Operations
        // -------------------------------------------------------------------------


        Page<OrderResponse> findOrdersWithPredicate(Predicate predicate, Pageable pageable);


        Page<OrderResponse> searchOrders(String keyword, Pageable pageable);

        Page<OrderResponse> filterOrders(OrderFilterInput filter, Pageable pageable);

        // -------------------------------------------------------------------------
        // Additional Convenience Methods
        // -------------------------------------------------------------------------


        Page<OrderResponse> getOrdersByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);


        Page<OrderResponse> getHighValueOrders(BigDecimal threshold, Pageable pageable);

        Page<OrderResponse> getOverdueOrders(java.time.LocalDateTime cutoffDate, Pageable pageable);


        Page<OrderResponse> getOrdersByDateRange(java.time.LocalDateTime startDate,
                                                 java.time.LocalDateTime endDate,
                                                 Pageable pageable);

        boolean existsByIdAndActive(Long orderId);

        long countByUserId(Long userId);


        long countByStatus(OrderStatus status);

        // -------------------------------------------------------------------------
        // Admin Convenience Methods
        // -------------------------------------------------------------------------

        Page<OrderResponse> getOrdersNeedingAttention(Pageable pageable);

        Page<OrderResponse> getCompletedOrders(Pageable pageable);

        Page<OrderResponse> getPaidOrders(Pageable pageable);

        Page<OrderResponse> getOrdersWithTracking(Pageable pageable);

        OrderResponse getOrderById(Long orderId);
}