package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductFilterRequest;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * QueryDSL Predicate builder for Product entity
 * Provides type-safe, compile-time checked queries for complex product filtering
 *
 * Usage:
 * <pre>
 * Predicate predicate = ProductPredicates.builder()
 *     .withNameContaining("Laptop")
 *     .withCategoryId(5L)
 *     .withPriceBetween(new BigDecimal("1000"), new BigDecimal("2000"))
 *     .withActive(true)
 *     .build();
 *
 * productRepository.findAll(predicate, pageable);
 * </pre>
 */
public class ProductPredicates {
    private final BooleanBuilder builder;

    public ProductPredicates() {
        this.builder = new BooleanBuilder();
    }

    public static ProductPredicates builder() {
        return new ProductPredicates();
    }

    public static Predicate fromFilterRequest(@Valid ProductFilterRequest filter) {
        if (filter == null) {
            return ProductPredicates.builder().withActive(true).build();
        }
        ProductPredicates builder = ProductPredicates.builder()
                .withNameContaining(filter.getName())
                .withSlug(filter.getSlug())
                .withSku(filter.getSku())
                .withCategoryId(filter.getCategoryId())
                .withCategoryIds(filter.getCategoryIds())
                .withCategorySlug(filter.getCategorySlug())
                .withPriceBetween(filter.getMinPrice(), filter.getMaxPrice())
                .withInventoryStatus(filter.getInventoryStatus() != null ? filter.getInventoryStatus().name() : null)
                .withInventoryStatuses(filter.getInventoryStatuses())
                .withFeatured(filter.getFeatured())
                .withActive(true)
                .withCreatedAfter(filter.getCreatedAfter())
                .withCreatedBefore(filter.getCreatedBefore())
                .withCreatedBetween(filter.getCreatedAfter(), filter.getCreatedBefore())
                .withStockGreaterThan(filter.getMinStock())
                .withStockLessThan(filter.getMaxStock())
                .withStockBetween(filter.getMinStock(), filter.getMaxStock())
                .withRatingBetween(filter.getMinRating(), filter.getMaxRating())
                .withDiscounted(filter.getHasDiscount())
                .withNew(filter.getIsNew())
                .withBestseller(filter.getIsBestseller())
                .withSearch(filter.getKeyword())
                .withInStockOnly(filter.getInStockOnly() != null && filter.getInStockOnly())
                .withLowStockOnly(filter.getLowStockOnly() != null && filter.getLowStockOnly())
                .withOutOfStockOnly(filter.getOutOfStockOnly() != null && filter.getOutOfStockOnly())
                .withNeedsReorderOnly(filter.getNeedsReorderOnly() != null && filter.getNeedsReorderOnly());
        if (filter.getPopular() != null && filter.getPopular()) {
            builder.withPopular(100L);
        }
        if (filter.getTrending() != null && filter.getTrending()) {
            builder.withTrending(10L);
        }
        return builder.build();
    }

    public ProductPredicates withNameContaining(String name) {
        if (name != null && !name.isEmpty()) {
            builder.and(Expressions.stringPath("name").containsIgnoreCase(name));
        }
        return this;
    }


    public ProductPredicates withSlug(String slug) {
        if (slug != null && !slug.isEmpty()) {
            builder.and(Expressions.stringPath("slug").equalsIgnoreCase(slug));
        }
        return this;
    }

    public ProductPredicates withSku(String sku) {
        if (sku != null && !sku.isEmpty()) {
            builder.and(Expressions.stringPath("sku").equalsIgnoreCase(sku));
        }
        return this;
    }

    public ProductPredicates withCategoryId(Long categoryId) {
        if (categoryId != null) {
            // Use category.id path for the relationship
            builder.and(Expressions.numberPath(Long.class, "category.id").eq(categoryId));
        }
        return this;
    }

    public ProductPredicates withCategorySlug(String categorySlug) {
        if (categorySlug != null && !categorySlug.isEmpty()) {
            // Use category.slug path for the relationship
            builder.and(Expressions.stringPath("category.slug").equalsIgnoreCase(categorySlug));
        }
        return this;
    }

    public ProductPredicates withPriceBetween(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "price").between(min, max));
        } else if (min != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "price").goe(min));
        } else if (max != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "price").loe(max));
        }
        return this;
    }

    public ProductPredicates withInventoryStatus(String status) {
        if (status != null && !status.isEmpty()) {
            builder.and(Expressions.stringPath("inventoryStatus").equalsIgnoreCase(status));
        }
        return this;
    }

    public ProductPredicates withFeatured(Boolean featured) {
        if (featured != null) {
            builder.and(Expressions.booleanPath("featured").eq(featured));
        }
        return this;
    }

    public ProductPredicates withActive(Boolean active) {
        if (active != null) {
            builder.and(Expressions.booleanPath("isActive").eq(active));
        } else {
            builder.and(Expressions.booleanPath("isActive").isTrue());
        }
        return this;
    }

    public ProductPredicates withCreatedAfter(LocalDateTime date) {
        if (date != null) {
            builder.and(Expressions.dateTimePath(LocalDateTime.class, "createdAt").goe(date));
        }
        return this;
    }

    public ProductPredicates withCreatedBefore(LocalDateTime date) {
        if (date != null) {
            builder.and(Expressions.dateTimePath(LocalDateTime.class, "createdAt").loe(date));
        }
        return this;
    }

    public ProductPredicates withCreatedBetween(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            builder.and(Expressions.dateTimePath(LocalDateTime.class, "createdAt").between(start, end));
        }
        return this;
    }

    public ProductPredicates withStockGreaterThan(Integer minStock) {
        if (minStock != null) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity").gt(minStock));
        }
        return this;
    }

    public ProductPredicates withStockLessThan(Integer maxStock) {
        if (maxStock != null) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity").lt(maxStock));
        }
        return this;
    }

    public ProductPredicates withDiscounted(Boolean discounted) {
        if (discounted != null) {
            if (discounted) {
                builder.and(Expressions.numberPath(BigDecimal.class, "discountPrice").isNotNull().and(Expressions.numberPath(BigDecimal.class, "discountPrice").gt(BigDecimal.ZERO)));
            } else {
                builder.and(Expressions.numberPath(BigDecimal.class, "discountPrice").isNull().or(Expressions.numberPath(BigDecimal.class, "discountPrice").eq(BigDecimal.ZERO)));
            }
        }
        return this;
    }

    public ProductPredicates withNew(Boolean isNew) {
        if (isNew != null) {
            builder.and(Expressions.booleanPath("isNew").eq(isNew));
        }
        return this;
    }

    public ProductPredicates withBestseller(Boolean isBestseller) {
        if (isBestseller != null) {
            builder.and(Expressions.booleanPath("isBestseller").eq(isBestseller));
        }
        return this;
    }

    public ProductPredicates withSearch(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            builder.and(Expressions.stringPath("name").containsIgnoreCase(keyword).or(Expressions.stringPath("description").containsIgnoreCase(keyword)).or(Expressions.stringPath("slug").containsIgnoreCase(keyword)).or(Expressions.stringPath("sku").containsIgnoreCase(keyword)));
        }
        return this;
    }

    public Predicate build() {
        return builder.getValue();
    }


    public ProductPredicates withInStockOnly(boolean b) {
        if (b) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity")
                .subtract(Expressions.numberPath(Integer.class, "reservedQuantity"))
                .gt(0));
        }
        return this;
    }

    public ProductPredicates withLowStockOnly(boolean b) {
        if (b) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity")
                .loe(Expressions.numberPath(Integer.class, "lowStockThreshold")));
        }
        return this;
    }

    public ProductPredicates withOutOfStockOnly(boolean b) {
        if (b) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity")
                .subtract(Expressions.numberPath(Integer.class, "reservedQuantity"))
                .loe(0));
        }
        return this;
    }

    public ProductPredicates withNeedsReorderOnly(boolean b) {
        if (b) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity")
                .loe(Expressions.numberPath(Integer.class, "reorderPoint")));
        }
        return this;
    }

    public ProductPredicates withPopular(long minViews) {
        // Popular: viewCount >= minViews
        builder.and(Expressions.numberPath(Long.class, "viewCount").goe(minViews));
        return this;
    }

    public ProductPredicates withTrending(long minSales) {
        // Trending: salesCount >= minSales
        builder.and(Expressions.numberPath(Long.class, "salesCount").goe(minSales));
        return this;
    }

    public ProductPredicates withCategoryIds(List<Long> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            builder.and(Expressions.numberPath(Long.class, "category.id").in(categoryIds));
        }
        return this;
    }

    public ProductPredicates withEffectivePriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        // NOTE: QueryDSL coalesce requires QProduct model, so fallback to price only
        if (minPrice != null && maxPrice != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "price").between(minPrice, maxPrice));
        } else if (minPrice != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "price").goe(minPrice));
        } else if (maxPrice != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "price").loe(maxPrice));
        }
        return this;
    }

    public ProductPredicates withRatingBetween(BigDecimal minRating, BigDecimal maxRating) {
        if (minRating != null && maxRating != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "ratingAverage").between(minRating, maxRating));
        } else if (minRating != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "ratingAverage").goe(minRating));
        } else if (maxRating != null) {
            builder.and(Expressions.numberPath(BigDecimal.class, "ratingAverage").loe(maxRating));
        }
        return this;
    }

    public ProductPredicates withInventoryStatuses(List<InventoryStatus> inventoryStatuses) {
        if (inventoryStatuses != null && !inventoryStatuses.isEmpty()) {
            builder.and(
                Expressions.stringPath("inventoryStatus").in(
                    inventoryStatuses.stream().map(Enum::name).toArray(String[]::new)
                )
            );
        }
        return this;
    }

    public ProductPredicates withStockBetween(Integer minStock, Integer maxStock) {
        if (minStock != null && maxStock != null) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity").between(minStock, maxStock));
        } else if (minStock != null) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity").goe(minStock));
        } else if (maxStock != null) {
            builder.and(Expressions.numberPath(Integer.class, "stockQuantity").loe(maxStock));
        }
        return this;
    }
}