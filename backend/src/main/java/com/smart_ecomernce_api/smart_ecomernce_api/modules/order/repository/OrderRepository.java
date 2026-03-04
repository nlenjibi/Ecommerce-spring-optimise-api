package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Order}.
 *
 * <p>
 * Naming notes:
 * <ul>
 * <li>The entity field is {@code status} (not {@code orderStatus}) — all
 * derived-query method names use {@code ...AndStatus...} accordingly.</li>
 * <li>{@code @EntityGraph} is applied to single-entity lookups to avoid N+1
 * queries when callers access {@code orderItems} or {@code user}.</li>
 * </ul>
 */
@Repository
public interface OrderRepository extends BaseRepository<Order, Long> {

  // -------------------------------------------------------------------------
  // Single-order lookups
  // -------------------------------------------------------------------------
  @EntityGraph(attributePaths = { "orderItems", "orderItems.product", "user" })
  Optional<Order> findByIdAndIsActiveTrue(Long id);

  @EntityGraph(attributePaths = { "orderItems", "orderItems.product", "user" })
  Optional<Order> findByOrderNumberAndIsActiveTrue(String orderNumber);

  boolean existsByOrderNumberAndIsActiveTrue(String orderNumber);

  // -------------------------------------------------------------------------
  // User-scoped paged queries
  // -------------------------------------------------------------------------

  Page<Order> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

  Page<Order> findByUserIdAndStatusAndIsActiveTrue(Long userId,
      OrderStatus status,
      Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.user.id  = :userId
        AND o.createdAt BETWEEN :start AND :end
      ORDER BY o.createdAt DESC
      """)
  Page<Order> findByUserAndDateRange(@Param("userId") Long userId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      Pageable pageable);

  // -------------------------------------------------------------------------
  // Admin / global paged queries
  // -------------------------------------------------------------------------

  Page<Order> findByStatusAndIsActiveTrue(OrderStatus status, Pageable pageable);

  Page<Order> findByPaymentStatusAndIsActiveTrue(PaymentStatus paymentStatus,
      Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.totalAmount BETWEEN :min AND :max
      """)
  Page<Order> findByAmountRange(@Param("min") BigDecimal min,
      @Param("max") BigDecimal max,
      Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND (LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :kw, '%'))
         OR  LOWER(o.user.email)  LIKE LOWER(CONCAT('%', :kw, '%'))
         OR  LOWER(o.customerName) LIKE LOWER(CONCAT('%', :kw, '%')))
      """)
  Page<Order> searchOrders(@Param("kw") String keyword, Pageable pageable);

  // -------------------------------------------------------------------------
  // Advanced filtering queries
  // -------------------------------------------------------------------------

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.totalAmount >= :threshold
      ORDER BY o.totalAmount DESC
      """)
  Page<Order> findHighValueOrders(@Param("threshold") BigDecimal threshold, Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.status IN ('PENDING', 'PROCESSING')
        AND o.createdAt <= :cutoffDate
      ORDER BY o.createdAt ASC
      """)
  Page<Order> findOverdueOrders(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.trackingNumber IS NOT NULL
      ORDER BY o.shippedAt DESC
      """)
  Page<Order> findOrdersWithTracking(Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.paymentStatus IN ('PAID', 'COMPLETED')
      ORDER BY o.paidAt DESC
      """)
  Page<Order> findPaidOrders(Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.status = 'DELIVERED'
      ORDER BY o.deliveredAt DESC
      """)
  Page<Order> findCompletedOrders(Pageable pageable);

  // -------------------------------------------------------------------------
  // Counts
  // -------------------------------------------------------------------------

  long countByUserIdAndIsActiveTrue(Long userId);

  long countByStatusAndIsActiveTrue(OrderStatus status);

  long countByUserIdAndStatusAndIsActiveTrue(Long userId, OrderStatus status);

  long countByPaymentStatusAndIsActiveTrue(PaymentStatus paymentStatus);

  @Query("""
      SELECT COUNT(o) FROM Order o
      WHERE o.isActive = true
        AND o.totalAmount >= :threshold
      """)
  long countHighValueOrders(@Param("threshold") BigDecimal threshold);

  // -------------------------------------------------------------------------
  // Statistics aggregations
  // -------------------------------------------------------------------------

  /**
   * Returns rows of [OrderStatus, count, sum(totalAmount)] grouped by status.
   */
  @Query("""
      SELECT o.status, COUNT(o), SUM(o.totalAmount)
      FROM Order o
      WHERE o.isActive = true
      GROUP BY o.status
      """)
  List<Object[]> getOrderStatisticsByStatus();

  /**
   * Returns rows of [OrderStatus, count, sum(totalAmount)] for a single user.
   */
  @Query("""
      SELECT o.status, COUNT(o), SUM(o.totalAmount)
      FROM Order o
      WHERE o.isActive = true
        AND o.user.id = :userId
      GROUP BY o.status
      """)
  List<Object[]> getOrderStatisticsByUser(@Param("userId") Long userId);

  /**
   * Returns rows of [PaymentStatus, count, sum(totalAmount)] grouped by payment
   * status.
   */
  @Query("""
      SELECT o.paymentStatus, COUNT(o), SUM(o.totalAmount)
      FROM Order o
      WHERE o.isActive = true
      GROUP BY o.paymentStatus
      """)
  List<Object[]> getOrderStatisticsByPaymentStatus();

  @Query("""
      SELECT SUM(o.totalAmount)
      FROM Order o
      WHERE o.isActive = true
        AND o.status = 'DELIVERED'
      """)
  BigDecimal getTotalRevenue();

  /**
   * Alias for getTotalRevenue for service compatibility
   */
  default BigDecimal calculateTotalRevenue() {
    return getTotalRevenue();
  }

  /**
   * Check if a user has a delivered order for a specific product
   */
  @Query("""
      SELECT COUNT(o) > 0
      FROM Order o
      JOIN o.orderItems oi
      WHERE o.isActive = true
        AND o.user.id = :userId
        AND oi.product.id = :productId
        AND o.status = 'DELIVERED'
      """)
  boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

  @Query("""
      SELECT SUM(o.totalAmount)
      FROM Order o
      WHERE o.isActive = true
        AND o.status = 'DELIVERED'
        AND o.createdAt BETWEEN :start AND :end
      """)
  BigDecimal getRevenueBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query("""
      SELECT COUNT(o)
      FROM Order o
      WHERE o.isActive = true
        AND o.status = 'DELIVERED'
      """)
  long countDelivered();

  @Query("""
      SELECT COUNT(o)
      FROM Order o
      WHERE o.isActive = true
        AND o.status IN ('PENDING', 'PROCESSING')
        AND o.createdAt <= :cutoff
      """)
  long countOverdueOrders(@Param("cutoff") LocalDateTime cutoff);

  // -------------------------------------------------------------------------
  // Revenue and analytics queries
  // -------------------------------------------------------------------------

  @Query("""
      SELECT YEAR(o.createdAt), MONTH(o.createdAt), COUNT(o), SUM(o.totalAmount)
      FROM Order o
      WHERE o.isActive = true
        AND o.status = 'DELIVERED'
        AND o.createdAt BETWEEN :start AND :end
      GROUP BY YEAR(o.createdAt), MONTH(o.createdAt)
      ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC
      """)
  List<Object[]> getMonthlyRevenue(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query("""
      SELECT o.user.id, COUNT(o), SUM(o.totalAmount)
      FROM Order o
      WHERE o.isActive = true
        AND o.status = 'DELIVERED'
      GROUP BY o.user.id
      ORDER BY SUM(o.totalAmount) DESC
      """)
  List<Object[]> getTopCustomersByRevenue(Pageable pageable);

  @Query("""
      SELECT AVG(o.totalAmount)
      FROM Order o
      WHERE o.isActive = true
        AND o.status = 'DELIVERED'
      """)
  BigDecimal getAverageOrderValue();

  // -------------------------------------------------------------------------
  // Operational / background-job queries
  // -------------------------------------------------------------------------

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.status IN ('PENDING', 'PROCESSING')
      ORDER BY o.createdAt ASC
      """)
  List<Order> findOrdersNeedingAttention();

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.status = 'PROCESSING'
        AND o.createdAt < :cutoff
      """)
  List<Order> findProcessingOverdueOrders(@Param("cutoff") LocalDateTime cutoff);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.status = 'PENDING'
        AND o.createdAt < :cutoff
      """)
  List<Order> findPendingOverdueOrders(@Param("cutoff") LocalDateTime cutoff);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.status = 'SHIPPED'
        AND o.shippedAt IS NOT NULL
        AND o.deliveredAt IS NULL
        AND o.shippedAt < :cutoff
      """)
  List<Order> findShippedButNotDelivered(@Param("cutoff") LocalDateTime cutoff);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.paymentStatus = 'PENDING'
        AND o.createdAt < :cutoff
      """)
  List<Order> findPaymentPendingOrders(@Param("cutoff") LocalDateTime cutoff);

  // -------------------------------------------------------------------------
  // Custom find methods for specific business logic
  // -------------------------------------------------------------------------

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.user.id = :userId
        AND o.status = 'DELIVERED'
      ORDER BY o.deliveredAt DESC
      """)
  List<Order> findRecentDeliveredOrdersForUser(@Param("userId") Long userId, Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.couponCode IS NOT NULL
      ORDER BY o.createdAt DESC
      """)
  Page<Order> findOrdersWithCoupons(Pageable pageable);

  @Query("""
      SELECT o FROM Order o
      WHERE o.isActive = true
        AND o.refundAmount IS NOT NULL
      ORDER BY o.refundedAt DESC
      """)
  Page<Order> findRefundedOrders(Pageable pageable);

  // -------------------------------------------------------------------------
  // Existence checks and validation queries
  // -------------------------------------------------------------------------

  boolean existsByIdAndUserIdAndIsActiveTrue(Long id, Long userId);

  boolean existsByOrderNumberAndUserIdAndIsActiveTrue(String orderNumber, Long userId);

  @Query("""
      SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
      FROM Order o
      WHERE o.isActive = true
        AND o.user.id = :userId
        AND o.status IN ('PENDING', 'PROCESSING')
      """)
  boolean hasActiveOrders(@Param("userId") Long userId);

  // -------------------------------------------------------------------------
  // Bulk operations
  // -------------------------------------------------------------------------

  @Query("""
      UPDATE Order o SET o.status = 'CANCELLED', o.isActive = false, o.updatedAt = CURRENT_TIMESTAMP
      WHERE o.isActive = true
        AND o.status = 'PENDING'
        AND o.createdAt < :cutoff
      """)
  int cancelAbandonedOrders(@Param("cutoff") LocalDateTime cutoff);

  @Query("""
      UPDATE Order o SET o.status = 'PROCESSING', o.updatedAt = CURRENT_TIMESTAMP
      WHERE o.isActive = true
        AND o.status = 'PENDING'
        AND o.createdAt >= :cutoff
      """)
  int processPendingOrders(@Param("cutoff") LocalDateTime cutoff);

  // Change List<Order> to Page<Order> for findAllWithItems
  @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
  Page<Order> findAll(com.querydsl.core.types.Predicate predicate, Pageable pageable);
}
