package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.impl;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.AuthorizationException;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.OrderFilterInput;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.entity.Cart;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.cart.repository.CartRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.CartOrderRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderStatsResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.mapper.OrderMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.OrderRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.AsyncInventoryService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.OrderService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Primary implementation of {@link OrderService}.
 *
 * <p>Transaction strategy:
 * <ul>
 *   <li>Class-level {@code @Transactional(readOnly = true)} for all reads —
 *       reduces lock contention and enables Hibernate read-optimisations.</li>
 *   <li>Each mutating method overrides with {@code @Transactional} (readOnly = false).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository  userRepository;
    private final OrderMapper     orderMapper;
    private final ProductRepository productRepository;
    private final CartRepository  cartRepository;
    private final AsyncInventoryService asyncInventoryService;
    // Define cache names as constants
    private static final String CACHE_ORDER = "order";
    private static final String CACHE_ORDERS = "orders";
    private static final String CACHE_USER_ORDERS = "user-orders";
    private static final String CACHE_ORDER_STATS = "order-stats";
    private static final String CACHE_ORDER_COUNTS = "order-counts";
    private static final String CACHE_ORDERS_PREDICATE = "orders-predicate";
    private static final String CACHE_ORDERS_SEARCH = "orders-search";
    private static final String CACHE_ORDERS_FILTER = "orders-filter";


    @Override
    @Transactional
    @CacheEvict(value = {
            CACHE_ORDERS,
            CACHE_USER_ORDERS,
            CACHE_ORDER_STATS,
            CACHE_ORDER_COUNTS,
            CACHE_ORDERS_PREDICATE,
            CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER
    }, allEntries = true)
    public OrderResponse createOrderFromCart(Long cartId, Long userId, CartOrderRequest request) {

        // 1. Resolve user and cart, enforce ownership
        User user = findUserOrThrow(userId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));


        // === INVENTORY VALIDATION ===
        cart.getItems().forEach(cartItem -> {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            if (!product.isInStock() || product.getAvailableQuantity() < quantity) {
                throw new IllegalStateException(String.format(
                        "Insufficient stock for product '%s'. Available: %d, Requested: %d",
                        product.getName(), product.getAvailableQuantity(), quantity));
            }
            // Reserve stock (prevents race conditions)
            product.reserveStock(quantity);
            productRepository.save(product);
        });

        // 2. Delegate to the domain factory — this builds the Order + all OrderItems,
        //    copies coupon state, calls calculateTotals(), and generates the order number.
        Order order = Order.fromCart(cart, user);

        // 3. Layer on optional request fields (shipping, payment, notes).
        //    These are intentionally separate from the factory because they are
        //    concerns of the API layer, not the core domain checkout logic.
        if (request != null) {
            if (request.getShippingAddress() != null) {
                order.setShippingAddress(request.getShippingAddress());
            }
            if (request.getShippingMethod() != null) {
                order.setShippingMethod(request.getShippingMethod());
            }
            if (request.getPaymentMethod() != null) {
                order.setPaymentMethod(request.getPaymentMethod());
            }
            if (request.getCustomerNotes() != null) {
                order.setCustomerNotes(request.getCustomerNotes());
            }
            // Apply optional shipping cost override from the request
            if (request.getShippingCost() != null
                    && request.getShippingCost().compareTo(BigDecimal.ZERO) > 0) {
                order.applyShippingCost(request.getShippingCost()); // recalculates totals internally
            }
        }

        // 4. Persist — CascadeType.ALL on orderItems persists every OrderItem in one shot.
        Order saved = orderRepository.save(order);
        log.info("Order {} created from cart {} for user {} with {} items",
                saved.getOrderNumber(), cartId, userId, saved.getOrderItems().size());

        // === INVENTORY DEDUCTION ===
        saved.getOrderItems().forEach(orderItem -> {
            Product product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();
            product.deductStock(quantity);
            productRepository.save(product);
        });

        return orderMapper.toResponse(saved);
    }

    // =========================================================================
    // READ — single
    // =========================================================================

    @Override
    @Cacheable(value = CACHE_ORDER, key = "#id + '_' + #userId")
    public OrderResponse getOrderById(Long id, Long userId) {
        Order order = findActiveOrThrow(id);
        assertOwner(order, userId);
        return orderMapper.toResponse(order);
    }

    @Override
    @Cacheable(value = CACHE_ORDER, key = "#id + '_admin'")
    public OrderResponse getOrderByIdAsAdmin(Long id) {
        return orderMapper.toResponse(findActiveOrThrow(id));
    }

    @Override
    @Cacheable(value = CACHE_ORDER, key = "#orderId")
    public OrderResponse getOrderById(Long orderId) {
        return orderMapper.toResponse(findActiveOrThrow(orderId));
    }

    @Override
    @Cacheable(value = CACHE_ORDER, key = "#orderNumber")
    public OrderResponse getOrderByOrderNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumberAndIsActiveTrue(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + orderNumber));
        assertOwner(order, userId);
        return orderMapper.toResponse(order);
    }

    // =========================================================================
    // READ — paged
    // =========================================================================

    @Override
    @Cacheable(value = CACHE_USER_ORDERS, key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository
                .findByUserIdAndIsActiveTrue(userId, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_USER_ORDERS, key = "'user-status:' + #userId + ':' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getUserOrdersByStatus(Long userId,
                                                     OrderStatus status,
                                                     Pageable pageable) {
        return orderRepository
                .findByUserIdAndStatusAndIsActiveTrue(userId, status, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .build();
        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withStatus(status)
                .build();
        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Generic predicate-based filtered query.
     * All filter logic lives in {@link OrderPredicates#from(OrderFilterInput)}.
     */
    @Override
    @Cacheable(value = CACHE_ORDERS_FILTER,
            key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#filter=' + #filter.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<OrderResponse> getFilteredOrders(OrderFilterInput filter, Pageable pageable) {
        Predicate predicate = OrderPredicates.from(filter);
        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    // =========================================================================
    // STATUS TRANSITIONS
    // =========================================================================

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse confirmOrder(Long id) {
        Order order = findActiveOrThrow(id);
        order.confirm();
        log.info("Order {} confirmed", order.getOrderNumber());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse shipOrder(Long id, String trackingNumber, String carrier) {
        Order order = findActiveOrThrow(id);
        order.setTrackingNumber(trackingNumber);
        order.setCarrier(carrier);
        order.ship();
        log.info("Order {} shipped — tracking: {}", order.getOrderNumber(), trackingNumber);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse deliverOrder(Long id) {
        Order order = findActiveOrThrow(id);
        order.deliver();
        OrderResponse response = orderMapper.toResponse(orderRepository.save(order));
        
        asyncInventoryService.updateInventoryOnOrderDeliver(id);
        
        log.info("Order {} delivered", order.getOrderNumber());
        return response;
    }

    /**
     * Transition a CONFIRMED order → PROCESSING.
     * Guards are enforced by {@link Order#process()}.
     */
    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse processOrder(Long id) {
        Order order = findActiveOrThrow(id);
        order.process();
        OrderResponse response = orderMapper.toResponse(orderRepository.save(order));
        
        asyncInventoryService.updateInventoryOnOrderConfirm(id);
        
        log.info("Order {} moved to PROCESSING", order.getOrderNumber());
        return response;
    }

    /**
     * Transition a SHIPPED order → OUT_FOR_DELIVERY.
     * Guards are enforced by {@link Order#outForDelivery()}.
     */
    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse outForDeliveryOrder(Long id) {
        Order order = findActiveOrThrow(id);
        order.outForDelivery();
        log.info("Order {} is OUT_FOR_DELIVERY", order.getOrderNumber());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse cancelOrder(Long id, String reason, Long userId) {
        Order order = findActiveOrThrow(id);
        assertOwner(order, userId);
        order.cancel(reason);
        OrderResponse response = orderMapper.toResponse(orderRepository.save(order));
        
        asyncInventoryService.updateInventoryOnOrderCancel(id);
        
        log.info("Order {} cancelled by user {}: {}", order.getOrderNumber(), userId, reason);
        return response;
    }

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse refundOrder(Long id, BigDecimal amount, String reason) {
        Order order = findActiveOrThrow(id);
        order.refund(amount, reason);
        log.info("Order {} refunded — amount: {}", order.getOrderNumber(), amount);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse updateOrderStatus(Long id, OrderUpdateRequest request) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        Order order = findActiveOrThrow(id);

        OrderStatus newStatus = request.getStatus();
        if (newStatus == OrderStatus.CONFIRMED) {
            order.confirm();
        } else if (newStatus == OrderStatus.PROCESSING) {
            order.process();
        } else if (newStatus == OrderStatus.SHIPPED) {
            if (request.getTrackingNumber() != null) {
                order.setTrackingNumber(request.getTrackingNumber());
            }
            if (request.getCarrier() != null) {
                order.setCarrier(request.getCarrier());
            }
            order.ship();
        } else if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            order.outForDelivery();
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.deliver();
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.cancel(request.getCancellationReason() != null
                    ? request.getCancellationReason()
                    : "Cancelled by admin");
        } else if (newStatus == OrderStatus.REFUNDED) {
            order.refund(request.getRefundAmount() != null
                            ? request.getRefundAmount()
                            : order.getTotalAmount(),
                    request.getRefundReason() != null
                            ? request.getRefundReason()
                            : "Refunded by admin");
        } else {
            throw new IllegalArgumentException("Unsupported status transition: " + newStatus);
        }

        log.info("Order {} status updated to {} by admin", order.getOrderNumber(), request.getStatus());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#orderId")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse updatePaymentStatus(Long orderId, String status) {
        Order order = findActiveOrThrow(orderId);
        PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());

        if (paymentStatus == PaymentStatus.PAID) {
            order.markAsPaid(null);
        } else if (paymentStatus == PaymentStatus.FAILED) {
            order.markPaymentFailed();
        } else {
            order.setPaymentStatus(paymentStatus);
        }

        log.info("Order {} payment status updated to {}", order.getOrderNumber(), paymentStatus);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#id")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse updateOrderAsCustomer(Long id, OrderUpdateRequest request, Long userId) {
        Order order = findActiveOrThrow(id);
        assertOwner(order, userId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Customers may only update PENDING orders");
        }
        orderMapper.applyCustomerUpdate(order, request);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    // =========================================================================
    // ORDER ITEM OPERATIONS (Edit Pending Orders)
    // =========================================================================

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#orderId")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse addItemToOrder(Long orderId, Long productId, Integer quantity, Long userId) {
        Order order = findActiveOrThrow(orderId);
        assertOwner(order, userId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Items can only be added to PENDING orders");
        }

        // Use the entity helper to avoid re-streaming manually
        OrderItem existingItem = order.findItemByProductId(productId);

        if (existingItem != null) {
            // Product already in order — just bump the quantity.
            // getTotalPrice() now delegates to computeTotal(), so calculateTotals()
            // will pick up the new quantity correctly.
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));

            BigDecimal unitPrice = product.getEffectivePrice() != null
                    ? product.getEffectivePrice()
                    : product.getPrice();

            OrderItem newItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .discount(BigDecimal.ZERO)
                    .build();

            // addOrderItem sets the back-reference (item.order = order) as well
            order.addOrderItem(newItem);
        }

        // calculateTotals() now reads live computeTotal() values — no stale data
        order.calculateTotals();
        Order saved = orderRepository.save(order);
        log.info("Added item {} (qty: {}) to order {}", productId, quantity, orderId);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#orderId")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse removeItemFromOrder(Long orderId, Long productId, Long userId) {
        Order order = findActiveOrThrow(orderId);
        assertOwner(order, userId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Items can only be removed from PENDING orders");
        }

        var itemToRemove = order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order item for product", productId));

        order.removeOrderItem(itemToRemove);
        order.calculateTotals();
        Order saved = orderRepository.save(order);
        log.info("Removed item {} from order {}", productId, orderId);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CachePut(value = CACHE_ORDER, key = "#orderId")
    @CacheEvict(value = {
            CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public OrderResponse updateItemQuantity(Long orderId, Long productId, Integer quantity, Long userId) {
        Order order = findActiveOrThrow(orderId);
        assertOwner(order, userId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Items can only be modified in PENDING orders");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        var item = order.getOrderItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order item for product", productId));

        item.setQuantity(quantity);
        order.calculateTotals();
        Order saved = orderRepository.save(order);
        log.info("Updated item {} quantity to {} in order {}", productId, quantity, orderId);
        return orderMapper.toResponse(saved);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    @Transactional
    @CacheEvict(value = {
            CACHE_ORDER, CACHE_ORDERS, CACHE_ORDERS_PREDICATE, CACHE_ORDERS_SEARCH,
            CACHE_ORDERS_FILTER, CACHE_ORDER_STATS, CACHE_USER_ORDERS, CACHE_ORDER_COUNTS
    }, allEntries = true)
    public void deleteOrder(Long orderId) {
        if (!orderRepository.existsByIdAndIsActiveTrue(orderId)) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        orderRepository.deleteById(orderId);
        log.info("Order {} soft-deleted", orderId);
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    @Override
    //@Cacheable(value = "order-stats", key = "'global_v2'")
    public OrderStatsResponse getOrderStatistics() {
        // DEBUG: Simplified version
        return OrderStatsResponse.builder()
                .stats(OrderStats.builder()
                        .totalOrders(0L)
                        .totalRevenue(BigDecimal.ZERO)
                        .build())
                .build();
    }

    // =========================================================================
    // ADVANCED QUERY OPERATIONS
    // =========================================================================

    @Override
    @Cacheable(value = CACHE_ORDERS_PREDICATE,
            key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#predicate=' + #predicate.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<OrderResponse> findOrdersWithPredicate(Predicate predicate, Pageable pageable) {
        log.debug("Finding orders with predicate: {}", predicate);
        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_ORDERS_SEARCH,
            key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#keyword=' + #keyword + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<OrderResponse> searchOrders(String keyword, Pageable pageable) {
        log.debug("Searching orders with keyword: {}", keyword);

        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withSearch(keyword)
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_ORDERS_FILTER,
            key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#filter=' + #filter.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<OrderResponse> filterOrders(OrderFilterInput filter, Pageable pageable) {
        log.debug("Filtering orders with: {}", filter);

        Predicate predicate = OrderPredicates.from(filter);
        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'payment-status:' + #paymentStatus + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getOrdersByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable) {
        log.debug("Finding orders by payment status: {}", paymentStatus);

        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withPaymentStatus(paymentStatus)
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'high-value:' + #threshold + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getHighValueOrders(BigDecimal threshold, Pageable pageable) {
        log.debug("Finding high-value orders with threshold: {}", threshold);

        BigDecimal actualThreshold = threshold != null ? threshold : new BigDecimal("500.00");
        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withHighValue(actualThreshold)
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'overdue:' + T(org.springframework.util.DigestUtils).md5DigestAsHex(#cutoffDate.toString().getBytes()) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getOverdueOrders(LocalDateTime cutoffDate, Pageable pageable) {
        log.debug("Finding overdue orders with cutoff date: {}", cutoffDate);

        LocalDateTime actualCutoff = cutoffDate != null ? cutoffDate : LocalDateTime.now().minusDays(3);
        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withOverdueBefore(actualCutoff)
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'date-range:' + T(org.springframework.util.DigestUtils).md5DigestAsHex((#startDate.toString() + ':' + #endDate.toString()).getBytes()) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Finding orders between {} and {}", startDate, endDate);

        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withCreatedAfter(startDate)
                .withCreatedBefore(endDate)
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Cacheable(value = "order-exists", key = "#orderId")
    public boolean existsByIdAndActive(Long orderId) {
        return orderRepository.existsByIdAndIsActiveTrue(orderId);
    }

    @Override
    @Cacheable(value = CACHE_ORDER_COUNTS, key = "'user:' + #userId")
    public long countByUserId(Long userId) {
        return orderRepository.countByUserIdAndIsActiveTrue(userId);
    }

    @Override
    @Cacheable(value = CACHE_ORDER_COUNTS, key = "'status:' + #status")
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatusAndIsActiveTrue(status);
    }

    // =========================================================================
    // ADDITIONAL CONVENIENCE METHODS
    // =========================================================================

    /**
     * Get orders that need attention (PENDING, PROCESSING, PAYMENT_PENDING)
     */
    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'needing-attention:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getOrdersNeedingAttention(Pageable pageable) {
        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withOrdersNeedingAttention()
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Get completed orders (DELIVERED)
     */
    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'completed:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getCompletedOrders(Pageable pageable) {
        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withCompletedOrders()
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Get paid orders
     */
    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'paid:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getPaidOrders(Pageable pageable) {
        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withPaidOrders()
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Get orders with tracking numbers
     */
    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'with-tracking:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getOrdersWithTracking(Pageable pageable) {
        Predicate predicate = OrderPredicates.builder()
                .withActive(true)
                .withTrackingNumber()
                .build();

        return orderRepository.findAll(predicate, pageable)
                .map(orderMapper::toResponse);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Order findActiveOrThrow(Long id) {
        return orderRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthorizationException("User not found: " + userId));
    }

    private void assertOwner(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new AuthorizationException(
                    "Access denied to order " + order.getId());
        }
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    @Cacheable(value = CACHE_ORDERS, key = "'all:' + #status + ':' + #paymentStatus + ':' + #startDate + ':' + #endDate + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<OrderResponse> getAllOrders(OrderStatus status, PaymentStatus paymentStatus, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        OrderPredicates builder = OrderPredicates.builder().withActive(true);
        if (status != null) builder.withStatus(status);
        if (paymentStatus != null) builder.withPaymentStatus(paymentStatus);
        if (startDate != null) builder.withCreatedAfter(startDate);
        if (endDate != null) builder.withCreatedBefore(endDate);
        Predicate predicate = builder.build();
        return orderRepository.findAll(predicate, pageable).map(orderMapper::toResponse);
    }
}