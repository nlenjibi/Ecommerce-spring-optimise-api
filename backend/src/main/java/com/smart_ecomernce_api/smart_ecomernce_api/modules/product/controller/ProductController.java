package com.smart_ecomernce_api.smart_ecomernce_api.modules.product.controller;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.ProductPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.*;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.InventoryStatus;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product REST controller.
 *
 * Security model
 * ──────────────
 * Public storefront (read) endpoints → requireAuth = false
 * CRUD, stock management, bulk ops, admin inventory queries → ADMIN or MANAGER
 */
@Slf4j
@RestController
@RequestMapping("v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing products with advanced filtering")
public class ProductController {

    private final ProductService productService;

    // ─────────────────────────────────────────────────────────────────────────
    //  CRUD – ADMIN / MANAGER only
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Create product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            UriComponentsBuilder uriBuilder) {
        log.info("Creating product: {}", request.getName());
        ProductResponse response = productService.createProduct(request);
        var uri = uriBuilder.path("/api/v1/products/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(uri)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Update product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully",
                productService.updateProduct(id, request)));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public single-product lookups
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @GetMapping("/slug/{slug}")

    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    @GetMapping("/sku/{sku}")

    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySku(sku)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public catalogue / listing endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
        @Operation(summary = "Get all products with advanced predicate filtering")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0")   int            page,
            @RequestParam(defaultValue = "20")  int            size,
            @RequestParam(defaultValue = "id")  String         sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String        search,
            @RequestParam(required = false) Long          categoryId,
            @RequestParam(required = false) List<Long>    categoryIds,
            @RequestParam(required = false) String        categorySlug,
            @RequestParam(required = false) BigDecimal    minPrice,
            @RequestParam(required = false) BigDecimal    maxPrice,
            @RequestParam(required = false) InventoryStatus inventoryStatus,
            @RequestParam(required = false) Boolean       featured,
            @RequestParam(required = false) Boolean       isNew,
            @RequestParam(required = false) Boolean       isBestseller,
            @RequestParam(required = false) Boolean       hasDiscount,
            @RequestParam(required = false) Boolean       inStockOnly,
            @RequestParam(required = false) Boolean       lowStockOnly,
            @RequestParam(required = false) Boolean       needsReorderOnly,
            @RequestParam(required = false) BigDecimal    minRating,
            @RequestParam(required = false) BigDecimal    maxRating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // Check if any filter is provided
        boolean hasFilters = search != null || categoryId != null || (categoryIds != null && !categoryIds.isEmpty()) 
                || categorySlug != null || minPrice != null || maxPrice != null || inventoryStatus != null
                || featured != null || isNew != null || isBestseller != null || hasDiscount != null
                || inStockOnly != null || lowStockOnly != null || needsReorderOnly != null
                || minRating != null || maxRating != null || createdAfter != null || createdBefore != null;
        
        if (!hasFilters) {
            // No filters - use simple query that works
            return ResponseEntity.ok(ApiResponse.success(
                    PaginatedResponse.from(productService.getAllProducts(pageable))));
        }
        
        // Use category-specific repository methods when possible (more reliable than QueryDSL)
        if (categoryId != null && !hasComplexFilters(search, minPrice, maxPrice, inventoryStatus, featured, isNew, isBestseller, hasDiscount)) {
            // Simple category filter - use dedicated repository method
            return ResponseEntity.ok(ApiResponse.success(
                    PaginatedResponse.from(productService.getProductsByCategory(categoryId, pageable))));
        }
        
        if (categorySlug != null && !hasComplexFilters(search, minPrice, maxPrice, inventoryStatus, featured, isNew, isBestseller, hasDiscount)) {
            // Simple category filter by slug - use dedicated repository method
            return ResponseEntity.ok(ApiResponse.success(
                    PaginatedResponse.from(productService.getProductsByCategoryName(categorySlug, pageable))));
        }
        
        // Has complex filters - use predicate (works for some filters)
        ProductPredicates predicates = ProductPredicates.builder()
                .withActive(true)
                .withSearch(search)
                .withCategoryId(categoryId)
                .withCategoryIds(categoryIds)
                .withCategorySlug(categorySlug)
                .withEffectivePriceBetween(minPrice, maxPrice)
                .withInventoryStatus(String.valueOf(inventoryStatus))
                .withFeatured(featured)
                .withNew(isNew)
                .withBestseller(isBestseller)
                .withDiscounted(hasDiscount)
                .withRatingBetween(minRating, maxRating)
                .withCreatedBetween(createdAfter, createdBefore);
        if (Boolean.TRUE.equals(inStockOnly))     predicates.withInStockOnly(true);
        if (Boolean.TRUE.equals(lowStockOnly))    predicates.withLowStockOnly(true);
        if (Boolean.TRUE.equals(needsReorderOnly)) predicates.withNeedsReorderOnly(true);
        return ResponseEntity.ok(ApiResponse.success(
                PaginatedResponse.from(productService.findByPredicate(predicates.build(), pageable))));
    }
    
    private boolean hasComplexFilters(String search, BigDecimal minPrice, BigDecimal maxPrice, 
            InventoryStatus inventoryStatus, Boolean featured, Boolean isNew, Boolean isBestseller, Boolean hasDiscount) {
        return search != null || minPrice != null || maxPrice != null || inventoryStatus != null
                || featured != null || isNew != null || isBestseller != null || hasDiscount != null;
    }

    @PostMapping("/filter")

    @Operation(summary = "Filter products using a request body")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> filterProducts(
            @Valid @RequestBody ProductFilterRequest filter,
            @RequestParam(defaultValue = "0")   int            page,
            @RequestParam(defaultValue = "20")  int            size,
            @RequestParam(defaultValue = "id")  String         sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(ApiResponse.success(
                PaginatedResponse.from(productService.findByPredicate(
                        ProductPredicates.fromFilterRequest(filter), pageable))));
    }

    @GetMapping("/search")

    @Operation(summary = "Advanced product search with QueryDSL predicates")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> searchProducts(
            @QuerydslPredicate(root = Product.class) Predicate predicate,
            @RequestParam(defaultValue = "0")   int            page,
            @RequestParam(defaultValue = "20")  int            size,
            @RequestParam(defaultValue = "id")  String         sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(ApiResponse.success("Products searched successfully",
                PaginatedResponse.from(productService.findByPredicate(predicate, pageable))));
    }

    @GetMapping("/featured")

    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(
                productService.findByPredicate(
                        ProductPredicates.builder().withActive(true).withFeatured(true).build(), pageable))));
    }

    @GetMapping("/new")

    @Operation(summary = "Get new products")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getNewProducts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(
                productService.findByPredicate(
                        ProductPredicates.builder().withActive(true).withNew(true).build(), pageable))));
    }

    @GetMapping("/discounted")

    @Operation(summary = "Get discounted products")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getDiscountedProducts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "discountPercentage"));
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(
                productService.findByPredicate(
                        ProductPredicates.builder().withActive(true).withDiscounted(true).build(), pageable))));
    }

    @GetMapping("/category/{categoryId}")

    @Operation(summary = "Get products by category ID")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0")   int            page,
            @RequestParam(defaultValue = "20")  int            size,
            @RequestParam(defaultValue = "id")  String         sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(
                productService.findByPredicate(
                        ProductPredicates.builder().withActive(true).withCategoryId(categoryId).build(), pageable))));
    }

    @GetMapping("/inventory-status/{status}")

    @Operation(summary = "Get products by inventory status")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getProductsByInventoryStatus(
            @PathVariable InventoryStatus status,
            @RequestParam(defaultValue = "0")   int            page,
            @RequestParam(defaultValue = "20")  int            size,
            @RequestParam(defaultValue = "id")  String         sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(
                productService.findByPredicate(
                        ProductPredicates.builder().withActive(true)
                                .withInventoryStatus(String.valueOf(status)).build(), pageable))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Admin inventory queries – ADMIN / MANAGER only
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/admin/needs-reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get products needing reorder (Admin)")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getProductsNeedingReorder(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "stockQuantity"));
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(
                productService.findByPredicate(
                        ProductPredicates.builder().withActive(true).withNeedsReorderOnly(true).build(), pageable))));
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get product statistics (Admin)")
    public ResponseEntity<ApiResponse<ProductStatisticsResponse>> getProductStatistics() {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductStatistics()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Bulk operations – ADMIN / MANAGER only
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/admin/bulk/featured")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Bulk update featured status (Admin)")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateFeatured(
            @RequestParam List<Long> productIds,
            @RequestParam Boolean    featured) {
        log.info("Bulk updating featured={} for {} products", featured, productIds.size());
        productService.bulkUpdateFeatured(productIds, featured);
        return ResponseEntity.ok(ApiResponse.success("Featured status updated successfully", null));
    }

    @DeleteMapping("/admin/bulk")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Bulk delete products (Admin)")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(@RequestParam List<Long> productIds) {
        log.info("Bulk deleting {} products", productIds.size());
        productService.bulkDelete(productIds);
        return ResponseEntity.ok(ApiResponse.success("Products deleted successfully", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Stock management – ADMIN / MANAGER only
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/admin/{id}/reduce-stock")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Reduce product stock (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> reduceStock(
            @PathVariable Long id, @RequestParam Integer quantity) {
        return ResponseEntity.ok(ApiResponse.success("Stock reduced successfully",
                productService.reduceStock(id, quantity)));
    }

    @PostMapping("/admin/{id}/restore-stock")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Restore product stock (Admin)")
    public ResponseEntity<ApiResponse<Void>> restoreStock(
            @PathVariable Long id, @RequestParam Integer quantity) {
        productService.restoreStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock restored successfully", null));
    }

    @PostMapping("/admin/{id}/reserve-stock")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Reserve product stock (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> reserveStock(
            @PathVariable Long id, @RequestParam Integer quantity) {
        productService.reserveStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock reserved successfully",
                productService.getProductById(id)));
    }

    @PostMapping("/admin/{id}/release-stock")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Release reserved stock (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> releaseReservedStock(
            @PathVariable Long id, @RequestParam Integer quantity) {
        productService.releaseReservedStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Reserved stock released successfully",
                productService.getProductById(id)));
    }
}