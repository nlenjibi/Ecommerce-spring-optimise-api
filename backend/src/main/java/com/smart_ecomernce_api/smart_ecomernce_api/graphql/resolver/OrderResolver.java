package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto.OrderResponseDto;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.OrderFilterInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderStatsResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.OrderUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.PaymentStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GraphQL resolver for all Order queries and mutations.
 *
 * <p>userId is expected in the GraphQL context (populated by a WebGraphQlInterceptor
 * that reads the JWT and stores the principal's ID as a context value).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderResolver {

    private final OrderService orderService;

    // =========================================================================
    // Queries
    // =========================================================================

    @QueryMapping
    
    public OrderResponse order(@Argument Long id, @ContextValue Long userId) {
        log.debug("GQL order(id={})", id);
        return orderService.getOrderById(id, userId);
    }

    @QueryMapping
    
    public OrderResponse orderByNumber(@Argument String orderNumber,
                                       @ContextValue Long userId) {
        log.debug("GQL orderByNumber({})", orderNumber);
        return orderService.getOrderByOrderNumber(orderNumber, userId);
    }

    @QueryMapping
    
    public OrderResponseDto myOrders(@Argument PageInput pagination,
                                     @ContextValue Long userId) {
        log.debug("GQL myOrders(user={})", userId);
        Page<OrderResponse> page = orderService.getUserOrders(userId, toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto allOrders(@Argument PageInput pagination) {
        log.debug("GQL allOrders");
        Page<OrderResponse> page = orderService.getAllOrders(toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto ordersByStatus(@Argument OrderStatus status,
                                           @Argument PageInput pagination) {
        log.debug("GQL ordersByStatus(status={})", status);
        Page<OrderResponse> page = orderService.getOrdersByStatus(status, toPageable(pagination));
        return toDto(page);
    }

    /**
     * Admin-only query: combines any combination of filters via
     * {@link OrderPredicates}.
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto filteredOrders(@Argument OrderFilterInput filter,
                                           @Argument PageInput pagination) {
        log.debug("GQL filteredOrders(filter={})", filter);
        Page<OrderResponse> page = orderService.getFilteredOrders(filter, toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto filteredOrdersAdvanced(@Argument OrderFilterInput filter,
                                                   @Argument PageInput pagination) {
        log.debug("GQL filteredOrdersAdvanced(filter={})", filter);

        Pageable pageable = toPageable(pagination);
        Predicate predicate = OrderPredicates.from(filter);

        Page<OrderResponse> page = orderService.findOrdersWithPredicate(predicate, pageable);
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto searchOrders(@Argument String keyword,
                                         @Argument PageInput pagination) {
        log.debug("GQL searchOrders(keyword={})", keyword);

        Pageable pageable = toPageable(pagination);
        Page<OrderResponse> page = orderService.searchOrders(keyword, pageable);
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Cacheable(value = "orders", key = "'stats'")
    public OrderStatsResponse orderStatistics() {
        log.debug("GQL orderStatistics");
        return orderService.getOrderStatistics();
    }

    // =========================================================================
    // Advanced Query Operations
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto ordersByPaymentStatus(@Argument String paymentStatus,
                                                  @Argument PageInput pagination) {
        log.debug("GQL ordersByPaymentStatus(status={})", paymentStatus);

        PaymentStatus status = PaymentStatus.valueOf(paymentStatus.toUpperCase());
        Page<OrderResponse> page = orderService.getOrdersByPaymentStatus(status, toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto highValueOrders(@Argument BigDecimal threshold,
                                            @Argument PageInput pagination) {
        log.debug("GQL highValueOrders(threshold={})", threshold);

        Page<OrderResponse> page = orderService.getHighValueOrders(threshold, toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto overdueOrders(@Argument LocalDateTime cutoffDate,
                                          @Argument PageInput pagination) {
        log.debug("GQL overdueOrders(cutoffDate={})", cutoffDate);

        Page<OrderResponse> page = orderService.getOverdueOrders(cutoffDate, toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto ordersByDateRange(@Argument LocalDateTime startDate,
                                              @Argument LocalDateTime endDate,
                                              @Argument PageInput pagination) {
        log.debug("GQL ordersByDateRange({} to {})", startDate, endDate);

        Page<OrderResponse> page = orderService.getOrdersByDateRange(startDate, endDate, toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto ordersNeedingAttention(@Argument PageInput pagination) {
        log.debug("GQL ordersNeedingAttention");

        Page<OrderResponse> page = orderService.getOrdersNeedingAttention(toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto completedOrders(@Argument PageInput pagination) {
        log.debug("GQL completedOrders");

        Page<OrderResponse> page = orderService.getCompletedOrders(toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto paidOrders(@Argument PageInput pagination) {
        log.debug("GQL paidOrders");

        Page<OrderResponse> page = orderService.getPaidOrders(toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto ordersWithTracking(@Argument PageInput pagination) {
        log.debug("GQL ordersWithTracking");

        Page<OrderResponse> page = orderService.getOrdersWithTracking(toPageable(pagination));
        return toDto(page);
    }

    // =========================================================================
    // Count and Existence Queries
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Long countOrdersByUser(@Argument Long userId) {
        log.debug("GQL countOrdersByUser(userId={})", userId);
        return orderService.countByUserId(userId);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Long countOrdersByStatus(@Argument OrderStatus status) {
        log.debug("GQL countOrdersByStatus(status={})", status);
        return orderService.countByStatus(status);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Boolean orderExists(@Argument Long orderId) {
        log.debug("GQL orderExists(orderId={})", orderId);
        return orderService.existsByIdAndActive(orderId);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Boolean hasActiveOrders(@Argument Long userId) {
        log.debug("GQL hasActiveOrders(userId={})", userId);
        long count = orderService.countByUserId(userId);
        return count > 0;
    }


    @MutationMapping
    
    public OrderResponse cancelOrder(@Argument Long id,
                                     @Argument String reason,
                                     @ContextValue Long userId) {
        log.info("GQL cancelOrder(id={}, user={})", id, userId);
        return orderService.cancelOrder(id, reason, userId);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")

    public OrderResponse confirmOrder(@Argument Long id) {
        log.info("GQL confirmOrder(id={})", id);
        return orderService.confirmOrder(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")

    public OrderResponse shipOrder(@Argument Long id,
                                   @Argument String trackingNumber,
                                   @Argument String carrier) {
        log.info("GQL shipOrder(id={})", id);
        return orderService.shipOrder(id, trackingNumber, carrier);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")

    public OrderResponse deliverOrder(@Argument Long id) {
        log.info("GQL deliverOrder(id={})", id);
        return orderService.deliverOrder(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")

    public OrderResponse refundOrder(@Argument Long id,
                                     @Argument BigDecimal amount,
                                     @Argument String reason) {
        log.info("GQL refundOrder(id={})", id);
        return orderService.refundOrder(id, amount, reason);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")

    public OrderResponse updatePaymentStatus(@Argument Long id,
                                             @Argument String status) {
        log.info("GQL updatePaymentStatus(id={}, status={})", id, status);
        return orderService.updatePaymentStatus(id, status);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")

    public OrderResponse updateOrderStatus(@Argument Long id,
                                           @Argument OrderUpdateRequest request) {
        log.info("GQL updateOrderStatus(id={})", id);
        return orderService.updateOrderStatus(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")

    public Boolean deleteOrder(@Argument Long id) {
        log.info("GQL deleteOrder(id={})", id);
        orderService.deleteOrder(id);
        return true;
    }

    @MutationMapping
    
    public OrderResponse updateOrderAsCustomer(@Argument Long id,
                                               @Argument OrderUpdateRequest request,
                                               @ContextValue Long userId) {
        log.info("GQL updateOrderAsCustomer(id={}, user={})", id, userId);
        return orderService.updateOrderAsCustomer(id, request, userId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "orderDate"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }

    private OrderResponseDto toDto(Page<OrderResponse> page) {
        return OrderResponseDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    // =========================================================================
    // Subscription-like queries (for real-time updates)
    // =========================================================================

    @QueryMapping
    
    public OrderResponseDto recentOrders(@Argument PageInput pagination,
                                         @ContextValue Long userId) {
        log.debug("GQL recentOrders(user={})", userId);

        // Get recent orders (last 30 days)
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        Pageable pageable = toPageable(pagination);
        Page<OrderResponse> page = orderService.getOrdersByDateRange(startDate, endDate, pageable);
        return toDto(page);
    }

    @QueryMapping
    
    public OrderResponseDto myActiveOrders(@Argument PageInput pagination,
                                           @ContextValue Long userId) {
        log.debug("GQL myActiveOrders(user={})", userId);

        // Get orders that are not completed or cancelled
        OrderFilterInput filter = new OrderFilterInput();
        filter.setStatus("PENDING,PROCESSING,SHIPPED");
        filter.setUserId(userId);
        filter.setIsActive(true);

        Page<OrderResponse> page = orderService.getFilteredOrders(filter, toPageable(pagination));
        return toDto(page);
    }

    @QueryMapping
    
    public OrderResponseDto orderHistory(@Argument PageInput pagination,
                                         @ContextValue Long userId) {
        log.debug("GQL orderHistory(user={})", userId);

        // Get completed orders for user
        OrderFilterInput filter = new OrderFilterInput();
        filter.setStatus("DELIVERED");
        filter.setUserId(userId);
        filter.setIsActive(true);

        Page<OrderResponse> page = orderService.getFilteredOrders(filter, toPageable(pagination));
        return toDto(page);
    }
}