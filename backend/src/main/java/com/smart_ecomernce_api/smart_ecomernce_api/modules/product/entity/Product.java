package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.validator.ValidPriceRange;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enhanced Product Entity with JPA Optimizations
 * - Indexed columns for better query performance
 * - Lazy loading for relationships
 * - Formula-based calculated fields
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_slug", columnList = "slug"),
        @Index(name = "idx_product_name", columnList = "name"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_sku", columnList = "sku"),
        @Index(name = "idx_product_status", columnList = "inventory_status"),
        @Index(name = "idx_product_stock", columnList = "stock_quantity"),
        @Index(name = "idx_product_featured", columnList = "featured"),
        @Index(name = "idx_product_active", columnList = "is_active"),
        @Index(name = "idx_product_price", columnList = "price"),
        @Index(name = "idx_product_discount_price", columnList = "discount_price"),
        @Index(name = "idx_product_created_at", columnList = "created_at"),
        @Index(name = "idx_product_composite", columnList = "category_id, is_active, inventory_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ValidPriceRange
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    @NotBlank(message = "Product name cannot be blank")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "slug", nullable = false, unique = true, length = 250)
    @NotBlank(message = "Product slug cannot be blank")
    private String slug;

    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    // ==================== Pricing Fields ====================

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @Column(name = "discount_price", precision = 10, scale = 2)
    @Positive(message = "Discount price must be positive")
    private BigDecimal discountPrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    @Positive(message = "Cost price must be positive")
    private BigDecimal costPrice;

    /**
     * Effective price calculated by database formula
     * Returns discount price if available, otherwise regular price
     */
    @Formula("COALESCE(discount_price, price)")
    private BigDecimal effectivePrice;

    /**
     * Discount percentage calculated by database
     */
    @Formula("CASE WHEN discount_price IS NOT NULL AND discount_price > 0 " +
            "THEN ROUND(((price - discount_price) / price) * 100, 2) " +
            "ELSE 0 END")
    private BigDecimal discountPercentage;

    // ==================== Inventory Fields ====================

    @Column(name = "stock_quantity", nullable = false)
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Min(value = 0, message = "Reserved quantity cannot be negative")
    @Builder.Default
    private Integer reservedQuantity = 0;

    /**
     * Available quantity calculated by database formula
     */
    @Formula("stock_quantity - reserved_quantity")
    private Integer availableQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(name = "reorder_point", nullable = false)
    @Min(value = 0, message = "Reorder point cannot be negative")
    @Builder.Default
    private Integer reorderPoint = 5;

    @Column(name = "reorder_quantity")
    @Positive(message = "Reorder quantity must be positive")
    private Integer reorderQuantity;

    @Column(name = "max_stock_quantity")
    @Positive(message = "Max stock quantity must be positive")
    private Integer maxStockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false, length = 20)
    @Builder.Default
    private InventoryStatus inventoryStatus = InventoryStatus.IN_STOCK;

    @Column(name = "track_inventory")
    @Builder.Default
    private Boolean trackInventory = true;

    @Column(name = "allow_backorder")
    @Builder.Default
    private Boolean allowBackorder = false;

    @Column(name = "expected_restock_date")
    private LocalDateTime expectedRestockDate;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    // ==================== Marketing Fields ====================

    @Column(name = "featured")
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "is_new")
    @Builder.Default
    private Boolean isNew = false;

    @Column(name = "is_bestseller")
    @Builder.Default
    private Boolean isBestseller = false;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "sales_count")
    @Builder.Default
    private Long salesCount = 0L;

    @Column(name = "rating_average", precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

    // ==================== Media Fields ====================

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_additional_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    @Builder.Default
    @BatchSize(size = 20)
    private List<String> additionalImages = new ArrayList<>();

    // ==================== SEO Fields ====================

    @Column(name = "meta_title", length = 200)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    @Builder.Default
    @BatchSize(size = 20)
    private Set<String> tags = new HashSet<>();

    // ==================== Relationships ====================

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Category is required")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @BatchSize(size = 20)
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<ProductImage> images = new ArrayList<>();


    // ==================== Business Logic Methods ====================

    public boolean isInStock() {
        if (!trackInventory)
            return true;
        return getAvailableQuantity() > 0;
    }



    public Integer getAvailableQuantity() {
        if (availableQuantity != null) {
            return availableQuantity;
        }
        return stockQuantity - reservedQuantity;
    }

    public BigDecimal getEffectivePrice() {
        if (effectivePrice != null) {
            return effectivePrice;
        }
        return discountPrice != null && discountPrice.compareTo(price) < 0
                ? discountPrice
                : price;
    }




    // ==================== Stock Management ====================

    public void reserveStock(int quantity) {
        if (!trackInventory)
            return;

        if (getAvailableQuantity() < quantity) {
            throw new IllegalStateException(
                    String.format("Insufficient stock. Available: %d, Requested: %d",
                            getAvailableQuantity(), quantity));
        }
        this.reservedQuantity += quantity;
        updateInventoryStatus();
    }

    public void releaseReservedStock(int quantity) {
        if (!trackInventory)
            return;
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
        updateInventoryStatus();
    }

    public void deductStock(int quantity) {
        if (!trackInventory)
            return;

        int releaseAmount = Math.min(quantity, this.reservedQuantity);
        this.reservedQuantity -= releaseAmount;

        if (this.stockQuantity < quantity) {
            throw new IllegalStateException(
                    String.format("Insufficient stock to deduct. Stock: %d, Requested: %d",
                            this.stockQuantity, quantity));
        }
        this.stockQuantity -= quantity;
        updateInventoryStatus();
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.stockQuantity += quantity;
        this.lastRestockedAt = LocalDateTime.now();
        updateInventoryStatus();
    }

    public void updateInventoryStatus() {
        if (!trackInventory) {
            this.inventoryStatus = InventoryStatus.IN_STOCK;
            return;
        }

        int available = getAvailableQuantity();

        if (available <= 0) {
            this.inventoryStatus = allowBackorder
                    ? InventoryStatus.BACKORDER
                    : InventoryStatus.OUT_OF_STOCK;
        } else if (available <= lowStockThreshold) {
            this.inventoryStatus = InventoryStatus.LOW_STOCK;
        } else {
            this.inventoryStatus = InventoryStatus.IN_STOCK;
        }
    }


    // ==================== Lifecycle Callbacks ====================

    @PrePersist
    @PreUpdate
    protected void validateAndUpdate() {
        updateInventoryStatus();

        if (reservedQuantity > stockQuantity) {
            throw new IllegalStateException(
                    "Reserved quantity cannot exceed stock quantity");
        }

        if (discountPrice != null && discountPrice.compareTo(price) >= 0) {
            throw new IllegalArgumentException(
                    "Discount price must be less than regular price");
        }

        if (metaTitle == null || metaTitle.isBlank()) {
            this.metaTitle = this.name;
        }

        if (metaDescription == null || metaDescription.isBlank()) {
            this.metaDescription = this.description != null && this.description.length() > 160
                    ? this.description.substring(0, 157) + "..."
                    : this.description;
        }
    }
}

