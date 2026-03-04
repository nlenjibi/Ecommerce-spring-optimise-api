package com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.Order;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.entity.OrderItem;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.repository.OrderRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.order.service.AsyncInventoryService;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncInventoryServiceImpl implements AsyncInventoryService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    @Async("taskExecutor")
    @Transactional
    public void updateInventoryOnOrderConfirm(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("Order not found for inventory update: {}", orderId);
                return;
            }

            List<OrderItem> items = order.getOrderItems();
            for (OrderItem item : items) {
                Product product = item.getProduct();
                if (product != null) {
                    Integer currentStock = product.getStockQuantity();
                    int reservedQuantity = item.getQuantity();
                    product.setStockQuantity(Math.max(0, currentStock - reservedQuantity));
                    product.updateInventoryStatus();
                    productRepository.save(product);
                    log.debug("Reduced stock for product {} by {} on order confirm", 
                            product.getId(), reservedQuantity);
                }
            }
            log.info("Inventory updated for order {} on confirm - {} items processed", 
                    orderId, items.size());
        } catch (Exception e) {
            log.error("Failed to update inventory on order confirm for order {}: {}", 
                    orderId, e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void updateInventoryOnOrderCancel(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("Order not found for inventory restore: {}", orderId);
                return;
            }

            List<OrderItem> items = order.getOrderItems();
            for (OrderItem item : items) {
                Product product = item.getProduct();
                if (product != null) {
                    Integer currentStock = product.getStockQuantity();
                    int cancelledQuantity = item.getQuantity();
                    product.setStockQuantity(currentStock + cancelledQuantity);
                    product.updateInventoryStatus();
                    productRepository.save(product);
                    log.debug("Restored stock for product {} by {} on order cancel", 
                            product.getId(), cancelledQuantity);
                }
            }
            log.info("Inventory restored for order {} on cancel - {} items processed", 
                    orderId, items.size());
        } catch (Exception e) {
            log.error("Failed to restore inventory on order cancel for order {}: {}", 
                    orderId, e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void updateInventoryOnOrderDeliver(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("Order not found for inventory finalization: {}", orderId);
                return;
            }

            List<OrderItem> items = order.getOrderItems();
            for (OrderItem item : items) {
                Product product = item.getProduct();
                if (product != null) {
                    Long currentSalesCount = product.getSalesCount() != null ? product.getSalesCount() : 0L;
                    product.setSalesCount(currentSalesCount + item.getQuantity());
                    productRepository.save(product);
                    log.debug("Updated sales count for product {} on order deliver", product.getId());
                }
            }
            log.info("Inventory finalized for order {} on deliver - {} items processed", 
                    orderId, items.size());
        } catch (Exception e) {
            log.error("Failed to finalize inventory on order deliver for order {}: {}", 
                    orderId, e.getMessage());
        }
    }
}
