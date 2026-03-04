package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.controller;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.CategoryPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.ApiResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs for managing product categories with advanced filtering")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;

    // ==================== Enhanced REST Endpoints with Predicates ====================

    @GetMapping
    @Operation(summary = "List all categories", description = "Retrieve paginated list of all categories with optional filtering")
    public ResponseEntity<ApiResponse<PaginatedResponse<CategoryResponse>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean hasProducts,
            @RequestParam(required = false) Integer minOrder,
            @RequestParam(required = false) Integer maxOrder,
            @RequestParam(required = false) Boolean hasImage) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Predicate predicate = CategoryPredicates.builder()
                .withNameContaining(name)
                .withDescriptionContaining(description)
                .withActive(isActive)
                .withFeatured(featured)
                .withProducts(hasProducts)
                .withSortOrderBetween(minOrder, maxOrder)
                .withImage(hasImage)
                .build();
        Page<Category> categories = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = categories.map(categoryService::mapToResponse);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", PaginatedResponse.from(responsePage)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search categories", description = "Search categories by keyword and optional filters")
    public ResponseEntity<ApiResponse<PaginatedResponse<CategoryResponse>>> searchCategories(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean hasProducts) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Predicate predicate = CategoryPredicates.builder()
                .withSearch(keyword, null, featured, hasProducts)
                .withActive(isActive)
                .build();
        Page<Category> searchResults = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = searchResults.map(categoryService::mapToResponse);
        return ResponseEntity.ok(ApiResponse.success("Categories searched successfully", PaginatedResponse.from(responsePage)));
    }

    @GetMapping("/created-between")
    @Operation(summary = "Get categories created in date range", description = "Filter categories created within a specific date range")
    public ResponseEntity<ApiResponse<PaginatedResponse<CategoryResponse>>> getCategoriesCreatedBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Predicate predicate = CategoryPredicates.builder()
                .withCreatedBetween(startDate, endDate)
                .build();
        Page<Category> categories = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = categories.map(categoryService::mapToResponse);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", PaginatedResponse.from(responsePage)));
    }

    // ==================== Existing Endpoints (Keep as is) ====================

    @PostMapping("/amdin")
    @PreAuthorize("hasAnyRole('ADMIN')")

    @Operation(summary = "Create a new category", description = "Create a new product category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            UriComponentsBuilder uriBuilder) {

        CategoryResponse response = categoryService.createCategory(request);


        return ResponseEntity.ok(ApiResponse.success("Category created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve category details by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id) {

        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", response));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")

    @Operation(summary = "Update a category", description = "Update category details")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {

        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", response));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")

    @Operation(summary = "Delete a category", description = "Delete category by ID")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieve category by its unique slug")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(
            @Parameter(description = "Category slug", required = true)
            @PathVariable String slug) {
        log.info("Fetching category with slug: {}", slug);
        CategoryResponse response = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", response));
    }

    @GetMapping("/active")
    @Operation(summary = "List all active categories", description = "Retrieve all active categories without pagination")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllActiveCategories() {
        List<CategoryResponse> response = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Active categories retrieved successfully", response));
    }

    @PatchMapping("/admin/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN')")

    @Operation(summary = "Activate category", description = "Set category as active")
    public ResponseEntity<ApiResponse<CategoryResponse>> activateCategory(
            @PathVariable Long id) {
        CategoryResponse response = categoryService.toggleCategoryStatus(id, true);
        return ResponseEntity.ok(ApiResponse.success("Category activated successfully", response));
    }

    @PatchMapping("/admin/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN')")

    @Operation(summary = "Deactivate category", description = "Set category as inactive")
    public ResponseEntity<ApiResponse<CategoryResponse>> deactivateCategory(
            @PathVariable Long id) {
        CategoryResponse response = categoryService.toggleCategoryStatus(id, false);
        return ResponseEntity.ok(ApiResponse.success("Category deactivated successfully", response));
    }


}
