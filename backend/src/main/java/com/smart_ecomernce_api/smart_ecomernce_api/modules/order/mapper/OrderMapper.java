package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStats;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    // =========================================================================
    // Order Entity → DTO
    // =========================================================================

    @Mapping(target = "userId",    source = "user.id")
    @Mapping(target = "items",     source = "orderItems")
    @Mapping(target = "itemCount", expression = "java(order.getItemCount())")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    default Page<OrderResponse> toResponsePage(Page<Order> orderPage) {
        return orderPage.map(this::toResponse);
    }

    // =========================================================================
    // OrderItem Entity → DTO
    // =========================================================================

    @Mapping(target = "productId",       expression = "java(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null)")
    @Mapping(target = "productImageUrl", source = "productImageUrl")
    OrderItemResponse toItemResponse(OrderItem orderItem);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> orderItems);

    // =========================================================================
    // OrderStats → DTO
    // =========================================================================

    default OrderStatsResponse toStatsResponse(OrderStats orderStats) {
        return OrderStatsResponse.builder()
                .stats(orderStats)
                .build();
    }

    // =========================================================================
    // DTO → Order Entity
    // =========================================================================

    /**
     * NOTE: The primary order-creation path (createOrder / createOrderFromCart)
     * builds the Order manually in the service layer.  This mapping exists for
     * completeness and is available if a mapper-based path is ever needed.
     *
     * <p>Known limitation: {@link #toOrderItemEntity} cannot resolve the real
     * product price from the DB because MapStruct interfaces have no Spring context.
     * The service always passes an explicit {@code unitPrice} in the request, so
     * {@code calculateItemTotal} uses it directly without touching the stub product.
     */
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "orderNumber",  expression = "java(generateOrderNumber())")
    @Mapping(target = "user",         source = "user")
    @Mapping(target = "status",       constant = "PENDING")
    @Mapping(target = "paymentStatus", constant = "PENDING")
    @Mapping(target = "orderDate",    expression = "java(java.time.LocalDateTime.now())")
    // BUG FIX: expression now calls mapItems() which returns List<OrderItem>.
    // Previously it wrapped the Set return in new ArrayList<>() — a symptom fix.
    // The real fix is that mapItems() itself now returns List<OrderItem>, matching
    // the Order.orderItems field declaration and avoiding silent item-collapsing.
    @Mapping(target = "orderItems",   expression = "java(mapItems(request.getItems(), user))")
    @Mapping(target = "isActive",     ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    Order toEntity(OrderCreateRequest request, User user);

    /**
     * Maps item create-requests to {@link OrderItem} entities.
     *
     * <p>BUG FIX: return type changed from {@code Set<OrderItem>} to
     * {@code List<OrderItem>} to match {@code Order.orderItems}.
     *
     * <p>Using a Set was a silent data-loss bug: all unsaved OrderItems have
     * {@code id == null}, so a Set would collapse every item after the first
     * into one entry (they all appear "equal" under any id-based equals/hashCode).
     * A List preserves all items unconditionally.
     */
    default List<OrderItem> mapItems(List<OrderItemCreateRequest> items, User user) {
        if (items == null || items.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return items.stream()
                .map(this::toOrderItemEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    // =========================================================================
    // Customer update mapping
    // =========================================================================

    /**
     * Applies a customer-originated update to an existing PENDING order.
     *
     * <p>Only non-sensitive, customer-editable fields on a PENDING order are
     * left un-ignored (currently: shippingAddress, shippingMethod, paymentMethod
     * — sourced from the shared {@link OrderUpdateRequest}).  Everything else is
     * explicitly locked down.
     *
     * <p>BUG FIX: {@code status} is now explicitly ignored.
     * Without this, MapStruct auto-maps {@code request.status → order.status}
     * directly, letting any customer PUT a status of SHIPPED, DELIVERED, REFUNDED,
     * etc., completely bypassing the entity's guarded state-machine methods
     * (confirm(), ship(), deliver(), …) and all their timestamp side-effects.
     * Status transitions must always go through the explicit service methods.
     */
    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "orderNumber",           ignore = true)
    @Mapping(target = "user",                  ignore = true)
    @Mapping(target = "customerEmail",         ignore = true)
    @Mapping(target = "customerName",          ignore = true)
    @Mapping(target = "status",                ignore = true)   // ← BUG FIX: never auto-map status
    @Mapping(target = "orderDate",             ignore = true)
    @Mapping(target = "orderItems",            ignore = true)
    @Mapping(target = "subtotal",              ignore = true)
    @Mapping(target = "taxAmount",             ignore = true)
    @Mapping(target = "taxRate",               ignore = true)
    @Mapping(target = "shippingCost",          ignore = true)
    @Mapping(target = "discountAmount",        ignore = true)
    @Mapping(target = "totalAmount",           ignore = true)
    @Mapping(target = "couponCode",            ignore = true)
    @Mapping(target = "couponDiscount",        ignore = true)
    @Mapping(target = "paymentStatus",         ignore = true)
    @Mapping(target = "paymentMethod",         ignore = true)
    @Mapping(target = "paymentTransactionId",  ignore = true)
    @Mapping(target = "paidAt",                ignore = true)
    @Mapping(target = "deliveredAt",           ignore = true)
    @Mapping(target = "cancelledAt",           ignore = true)
    @Mapping(target = "cancellationReason",    ignore = true)
    @Mapping(target = "refundedAt",            ignore = true)
    @Mapping(target = "refundAmount",          ignore = true)
    @Mapping(target = "refundReason",          ignore = true)
    @Mapping(target = "shippingMethod",        ignore = true)
    @Mapping(target = "trackingNumber",        ignore = true)
    @Mapping(target = "carrier",               ignore = true)
    @Mapping(target = "shippedAt",             ignore = true)
    @Mapping(target = "estimatedDeliveryDate", ignore = true)
    @Mapping(target = "customerNotes",         ignore = true)
    @Mapping(target = "isActive",              ignore = true)
    @Mapping(target = "createdAt",             ignore = true)
    @Mapping(target = "updatedAt",             expression = "java(java.time.LocalDateTime.now())")
    void applyCustomerUpdate(@MappingTarget Order order, OrderUpdateRequest request);

    // =========================================================================
    // Admin update mapping
    // =========================================================================

    /**
     * Applies an admin-originated field update to an existing order.
     *
     * <p>BUG FIX: {@code status} is also ignored here for the same reason as in
     * {@link #applyCustomerUpdate}.  Admin status transitions go through
     * {@code OrderServiceImpl.updateOrderStatus()}'s switch-on-enum, which calls
     * the correct entity method and sets all associated timestamps (shippedAt,
     * deliveredAt, cancelledAt, etc.).  Direct {@code setStatus()} bypasses all of
     * that.
     *
     * <p>Fields that admins CAN update directly (trackingNumber, carrier,
     * shippingAddress) are intentionally left un-ignored so MapStruct auto-maps them.
     */
    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "orderNumber",           ignore = true)
    @Mapping(target = "user",                  ignore = true)
    @Mapping(target = "customerEmail",         ignore = true)
    @Mapping(target = "customerName",          ignore = true)
    @Mapping(target = "status",                ignore = true)   // ← BUG FIX: must go through state machine
    @Mapping(target = "orderDate",             ignore = true)
    @Mapping(target = "orderItems",            ignore = true)
    @Mapping(target = "subtotal",              ignore = true)
    @Mapping(target = "taxAmount",             ignore = true)
    @Mapping(target = "taxRate",               ignore = true)
    @Mapping(target = "shippingCost",          ignore = true)
    @Mapping(target = "discountAmount",        ignore = true)
    @Mapping(target = "totalAmount",           ignore = true)
    @Mapping(target = "couponCode",            ignore = true)
    @Mapping(target = "couponDiscount",        ignore = true)
    @Mapping(target = "paymentStatus",         ignore = true)
    @Mapping(target = "paymentMethod",         ignore = true)
    @Mapping(target = "paymentTransactionId",  ignore = true)
    @Mapping(target = "paidAt",               ignore = true)
    @Mapping(target = "deliveredAt",           ignore = true)
    @Mapping(target = "cancelledAt",           ignore = true)
    @Mapping(target = "cancellationReason",    ignore = true)
    @Mapping(target = "refundedAt",            ignore = true)
    @Mapping(target = "refundAmount",          ignore = true)
    @Mapping(target = "refundReason",          ignore = true)
    @Mapping(target = "shippingMethod",        ignore = true)
    @Mapping(target = "shippedAt",             ignore = true)
    @Mapping(target = "estimatedDeliveryDate", ignore = true)
    @Mapping(target = "customerNotes",         ignore = true)
    @Mapping(target = "isActive",              ignore = true)
    @Mapping(target = "createdAt",             ignore = true)
    // trackingNumber, carrier, shippingAddress: intentionally NOT ignored → auto-mapped by MapStruct
    @Mapping(target = "updatedAt",             expression = "java(java.time.LocalDateTime.now())")
    void applyAdminUpdate(@MappingTarget Order order, OrderUpdateRequest request);

    // =========================================================================
    // OrderItemCreateRequest → OrderItem entity
    // =========================================================================

    /**
     * Maps an {@link OrderItemCreateRequest} to an {@link OrderItem} entity.
     *
     * <p>BUG FIX: {@link #mapIdToProduct} no longer sets {@code effectivePrice = ZERO}.
     * Previously, if the caller omitted {@code unitPrice} in the request,
     * {@code calculateItemTotal} would compute {@code 0 × quantity = 0}, silently
     * zeroing the line total.  The stub product now carries only the ID; price
     * resolution is the service's responsibility.
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "order",       ignore = true)
    @Mapping(target = "product",     source = "productId", qualifiedByName = "mapIdToProduct")
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "totalPrice",  expression = "java(calculateItemTotal(request.getUnitPrice(), request.getQuantity(), request.getDiscount()))")
    OrderItem toOrderItemEntity(OrderItemCreateRequest request);

    /**
     * Creates a minimal Product stub carrying only the ID.
     *
     * <p>BUG FIX: removed {@code product.setEffectivePrice(BigDecimal.ZERO)}.
     * Setting effectivePrice to zero meant that any mapper-built item whose
     * request had a null unitPrice would silently total to zero — a pricing bug
     * that was invisible until an order was paid.  Price resolution must be done
     * by the service via {@code productRepository.findById()}.
     */
    @Named("mapIdToProduct")
    default Product mapIdToProduct(Long productId) {
        if (productId == null) return null;
        Product product = new Product();
        product.setId(productId);
        // Do NOT set effectivePrice — the service resolves the real price from the DB.
        return product;
    }

    // =========================================================================
    // AfterMapping hooks
    // =========================================================================

    /** Recalculates all monetary totals after the entity is fully populated. */
    @AfterMapping
    default void calculateOrderTotals(@MappingTarget Order order) {
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            order.calculateTotals();
        }
    }

    /** Defensive fallback: ensures an order number is always set. */
    @AfterMapping
    default void ensureOrderNumber(@MappingTarget Order order) {
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            order.setOrderNumber(generateOrderNumber());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    default BigDecimal calculateItemTotal(BigDecimal unitPrice, Integer quantity, BigDecimal discount) {
        if (unitPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(discount);
        }
        return total.max(BigDecimal.ZERO);
    }

    /**
     * BUG FIX: format changed from {@code ORD-<millis>-<random3>} to
     * {@code ORD-YYYYMMDD-XXXXXX}, matching {@code Order#generateOrderNumber()}.
     *
     * <p>Previously the mapper generated order numbers with a completely different
     * format (timestamp-based) while the entity used a date-based format, so
     * orders created through different paths were visually inconsistent and harder
     * to reason about in logs and reports.
     */
    default String generateOrderNumber() {
        String date   = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        String random = String.format("%06d", new Random().nextInt(999_999));
        return "ORD-" + date + "-" + random;
    }
}