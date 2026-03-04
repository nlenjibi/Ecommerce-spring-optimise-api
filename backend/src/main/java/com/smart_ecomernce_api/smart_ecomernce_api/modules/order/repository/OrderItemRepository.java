package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);

    /**
     * Returns [productId, productName, totalQuantitySold] for DELIVERED orders,
     * ordered by totalQuantitySold descending.
     *
     * <p>Note: the {@code LIMIT} clause is not portable JPQL — call this via
     * {@code PageRequest.of(0, limit)} at the service layer instead, or use a
     * {@code Pageable} parameter.
     */
    @Query("""
            SELECT oi.product.id, oi.product.name, SUM(oi.quantity) AS totalSold
            FROM OrderItem oi
            WHERE oi.order.status = 'DELIVERED'
            GROUP BY oi.product.id, oi.product.name
            ORDER BY totalSold DESC
            """)
    List<Object[]> findBestSellingProducts(org.springframework.data.domain.Pageable pageable);

    /**
     * Total quantity sold for a specific product.
     */
    @Query("""
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM OrderItem oi
            WHERE oi.product.id = :productId
              AND oi.order.status = 'DELIVERED'
            """)
    long countTotalSoldByProduct(@Param("productId") Long productId);
}