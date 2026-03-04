package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import java.time.LocalDateTime;

/**
 * QueryDSL Predicate builder for Category entity
 * Provides type-safe, compile-time checked queries for complex category filtering
 * 
 * Usage:
 * <pre>
 * Predicate predicate = CategoryPredicates.builder()
 *     .withNameContaining("Electronics")
 *     .withRootCategory(true)
 *     .withFeatured(true)
 *     .build();
 * 
 * categoryRepository.findAll(predicate, pageable);
 * </pre>
 */
public class CategoryPredicates {

    private final BooleanBuilder builder;

    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_PARENT = "parent";
    private static final String FIELD_FEATURED = "featured";
    private static final String FIELD_PRODUCTS_SIZE = "products.size";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_IMAGE_URL = "imageUrl";
    private static final String FIELD_IS_ACTIVE = "isActive";

    public CategoryPredicates() {
        this.builder = new BooleanBuilder();
    }

    /**
     * Create a new predicate builder
     */
    public static CategoryPredicates builder() {
        return new CategoryPredicates();
    }

    /**
     * Filter by name containing (case-insensitive)
     */
    public CategoryPredicates withNameContaining(String name) {
        if (name != null && !name.isEmpty()) {
            builder.and(Expressions.stringPath(FIELD_NAME)
                .containsIgnoreCase(name));
        }
        return this;
    }

    /**
     * Filter by description containing
     */
    public CategoryPredicates withDescriptionContaining(String description) {
        if (description != null && !description.isEmpty()) {
            builder.and(Expressions.stringPath(FIELD_DESCRIPTION)
                .containsIgnoreCase(description));
        }
        return this;
    }




    /**
     * Filter root categories (no parent)
     */
    public CategoryPredicates withRootCategory(Boolean isRoot) {
        if (isRoot != null) {
            if (isRoot) {
                builder.and(Expressions.path(Object.class, FIELD_PARENT).isNull());
            } else {
                builder.and(Expressions.path(Object.class, FIELD_PARENT).isNotNull());
            }
        }
        return this;
    }


    /**
     * Filter featured categories
     */
    public CategoryPredicates withFeatured(Boolean featured) {
        if (featured != null) {
            builder.and(Expressions.booleanPath(FIELD_FEATURED).eq(featured));
        }
        return this;
    }

    /**
     * Filter categories with products
     */
    public CategoryPredicates withProducts(Boolean hasProducts) {
        if (hasProducts != null) {
            if (hasProducts) {
                builder.and(Expressions.numberPath(Integer.class, FIELD_PRODUCTS_SIZE).gt(0));
            } else {
                builder.and(Expressions.numberPath(Integer.class, FIELD_PRODUCTS_SIZE).eq(0));
            }
        }
        return this;
    }

    /**
     * Filter by minimum product count
     */
    public CategoryPredicates withMinProductCount(Integer minCount) {
        if (minCount != null && minCount > 0) {
            builder.and(Expressions.numberPath(Integer.class, FIELD_PRODUCTS_SIZE).goe(minCount));
        }
        return this;
    }

    /**
     * Filter categories created after date
     */
    public CategoryPredicates withCreatedAfter(LocalDateTime date) {
        if (date != null) {
            builder.and(Expressions.dateTimePath(LocalDateTime.class, FIELD_CREATED_AT)
                .goe(date));
        }
        return this;
    }

    /**
     * Filter categories created before date
     */
    public CategoryPredicates withCreatedBefore(LocalDateTime date) {
        if (date != null) {
            builder.and(Expressions.dateTimePath(LocalDateTime.class, FIELD_CREATED_AT)
                .loe(date));
        }
        return this;
    }

    /**
     * Filter categories created between dates
     */
    public CategoryPredicates withCreatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            builder.and(Expressions.dateTimePath(LocalDateTime.class, FIELD_CREATED_AT)
                .between(startDate, endDate));
        }
        return this;
    }

    /**
     * Filter categories with image
     */
    public CategoryPredicates withImage(Boolean hasImage) {
        if (hasImage != null) {
            if (hasImage) {
                builder.and(Expressions.stringPath(FIELD_IMAGE_URL).isNotNull()
                    .and(Expressions.stringPath(FIELD_IMAGE_URL).ne("")));
            } else {
                builder.and(Expressions.stringPath(FIELD_IMAGE_URL).isNull()
                    .or(Expressions.stringPath(FIELD_IMAGE_URL).eq("")));
            }
        }
        return this;
    }

    /**
     * Filter by sort order range
     */
    public CategoryPredicates withSortOrderBetween(Integer minOrder, Integer maxOrder) {
        if (minOrder != null && maxOrder != null) {
            builder.and(Expressions.numberPath(Integer.class, "sortOrder")
                .between(minOrder, maxOrder));
        }
        return this;
    }

    /**
     * Filter categories with subcategories
     */
    public CategoryPredicates withSubcategories(Boolean hasSubcategories) {
        if (hasSubcategories != null) {
            if (hasSubcategories) {
                builder.and(Expressions.numberPath(Integer.class, "children.size").gt(0));
            } else {
                builder.and(Expressions.numberPath(Integer.class, "children.size").eq(0));
            }
        }
        return this;
    }


    /**
     * Filter top-level featured categories
     */
    public CategoryPredicates withFeaturedRootCategory() {
        builder.and(Expressions.path(Object.class, FIELD_PARENT).isNull())
            .and(Expressions.booleanPath(FIELD_FEATURED).isTrue())
            .and(Expressions.booleanPath(FIELD_IS_ACTIVE).isTrue());
        return this;
    }


    /**
     * Filter active categories only
     */
    public CategoryPredicates withActive(Boolean active) {
        if (active != null) {
            builder.and(Expressions.booleanPath(FIELD_IS_ACTIVE).eq(active));
        } else {
            builder.and(Expressions.booleanPath(FIELD_IS_ACTIVE).isTrue());
        }
        return this;
    }


    /**
     * Complex search combining multiple criteria
     */
    public CategoryPredicates withSearch(String keyword, Boolean isRoot, 
                                          Boolean featured, Boolean hasProducts) {
        if (keyword != null && !keyword.isEmpty()) {
            builder.and(
                Expressions.stringPath("name").containsIgnoreCase(keyword)
                    .or(Expressions.stringPath("description").containsIgnoreCase(keyword))
                    .or(Expressions.stringPath("slug").containsIgnoreCase(keyword))
            );
        }
        if (isRoot != null) {
            if (isRoot) {
                builder.and(Expressions.path(Object.class, "parent").isNull());
            } else {
                builder.and(Expressions.path(Object.class, "parent").isNotNull());
            }
        }
        if (featured != null) {
            builder.and(Expressions.booleanPath("featured").eq(featured));
        }
        if (hasProducts != null) {
            if (hasProducts) {
                builder.and(Expressions.numberPath(Integer.class, "products.size").gt(0));
            } else {
                builder.and(Expressions.numberPath(Integer.class, "products.size").eq(0));
            }
        }
        return this;
    }

    /**
     * Build the predicate
     */
    public Predicate build() {
        return builder.getValue();
    }

}
