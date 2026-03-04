package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface CategoryService {

    // Add this method for direct entity to response mapping
    CategoryResponse mapToResponse(Category category);

    // ... all existing methods remain the same ...

    // Create and Update
    CategoryResponse createCategory(CategoryCreateRequest request);

    CategoryResponse updateCategory(Long id, CategoryUpdateRequest request);

    CategoryResponse toggleCategoryStatus(Long id, boolean isActive);

    CategoryResponse toggleFeaturedStatus(Long id, boolean featured);

    void deleteCategory(Long id);

    // Read operations
    CategoryResponse getCategoryById(Long id);

    CategoryResponse getCategoryBySlug(String slug);

    List<CategoryResponse> getAllActiveCategories();

    List<CategoryResponse> getAllFeaturedCategories();

    Page<CategoryResponse> getAllCategories(Pageable pageable);

    Page<CategoryResponse> getActiveCategories(Pageable pageable);

    // Search and Filter
    Page<CategoryResponse> searchCategories(String keyword, Pageable pageable);

    Page<CategoryResponse> searchActiveCategories(String keyword, Pageable pageable);

    Page<CategoryResponse> filterCategories(
            String name,
            Boolean isActive,
            Boolean featured,
            Boolean hasProducts,
            Pageable pageable);

    Page<CategoryResponse> advancedFilterCategories(
            String name,
            String description,
            Boolean isActive,
            Boolean featured,
            Integer minOrder,
            Integer maxOrder,
            Pageable pageable);

    // Product-related
    Page<CategoryResponse> getCategoriesWithProducts(Pageable pageable);

    Page<CategoryResponse> getCategoriesByProductCount(Pageable pageable);

    // Date-based filtering
    Page<CategoryResponse> getCategoriesCreatedBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    Page<CategoryResponse> getCategoriesUpdatedBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    // Bulk operations
    List<CategoryResponse> getCategoriesByIds(List<Long> ids);

    List<CategoryResponse> getCategoriesBySlugs(List<String> slugs);
    long getActiveCategoryCount();
}
