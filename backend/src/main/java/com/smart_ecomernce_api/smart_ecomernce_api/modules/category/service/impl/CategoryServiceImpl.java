package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service.impl;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.ResourceNotFoundException;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.mapper.CategoryMapper;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    @CachePut(value = "categories", key = "#result.id")
    @CacheEvict(value = {
            "categories-list", "categories-paged",
            "categories-search", "categories-filter", "categories-stats"
    }, allEntries = true)
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        log.info("Creating new category: {}", request.getName());

        // Generate slug from name
        String slug = generateSlug(request.getName());

        // Check if slug already exists
        if (categoryRepository.existsBySlug(slug)) {
            slug = generateUniqueSlug(slug);
        }

        Category category = categoryMapper.toEntity(request);
        category.setSlug(slug);

        if (request.getDisplayOrder() == null) {
            category.setDisplayOrder(0);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", savedCategory.getId());

        return categoryMapper.toSimpleResponse(savedCategory);
    }

    @Override
    @Transactional
    @CachePut(value = "categories", key = "#id")
    @CacheEvict(value = {
            "categories-list", "categories-paged",
            "categories-search", "categories-filter", "categories-stats"
    }, allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        log.info("Updating category with ID: {}", id);

        Category category = findCategoryById(id);

        // Update slug if name changed
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            String newSlug = generateSlug(request.getName());
            if (!newSlug.equals(category.getSlug()) &&
                    categoryRepository.existsBySlugAndIdNot(newSlug, id)) {
                newSlug = generateUniqueSlug(newSlug);
            }
            category.setSlug(newSlug);
        }

        categoryMapper.updateEntityFromRequest(request, category);

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully: {}", updatedCategory.getId());

        return categoryMapper.toSimpleResponse(updatedCategory);
    }

    @Override
    @Transactional
    @CachePut(value = "categories", key = "#id")
    @CacheEvict(value = {
            "categories-list", "categories-paged",
            "categories-search", "categories-filter", "categories-stats"
    }, allEntries = true)
    public CategoryResponse toggleCategoryStatus(Long id, boolean isActive) {
        log.info("Toggling category status - ID: {}, Active: {}", id, isActive);

        Category category = findCategoryById(id);
        category.setIsActive(isActive);

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toSimpleResponse(updatedCategory);
    }

    @Override
    @Transactional
    @CachePut(value = "categories", key = "#id")
    @CacheEvict(value = {
            "categories-list", "categories-paged",
            "categories-search", "categories-filter", "categories-stats"
    }, allEntries = true)
    public CategoryResponse toggleFeaturedStatus(Long id, boolean featured) {
        log.info("Toggling category featured status - ID: {}, Featured: {}", id, featured);

        Category category = findCategoryById(id);
        category.setFeatured(featured);

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toSimpleResponse(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "categories", "categories-list", "categories-paged",
            "categories-search", "categories-filter", "categories-stats"
    }, allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Deleting category with ID: {}", id);

        Category category = findCategoryById(id);

        // Check if category has products
        long productCount = categoryRepository.countActiveProductsInCategory(id);
        if (productCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category with " + productCount + " active products. " +
                            "Please reassign or delete the products first."
            );
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully: {}", id);
    }

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Fetching category by ID: {}", id);
        Category category = findCategoryById(id);
        CategoryResponse response = categoryMapper.toSimpleResponse(category);
        response.setProductCount(categoryRepository.countActiveProductsInCategory(id));
        return response;
    }

    @Override
    @Cacheable(value = "categories", key = "#slug")
    public CategoryResponse getCategoryBySlug(String slug) {
        log.debug("Fetching category by slug: {}", slug);

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));

        CategoryResponse response = categoryMapper.toSimpleResponse(category);
        response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
        return response;
    }

    @Override
    @Cacheable(value = "categories-list", key = "'all_active_categories'")
    public List<CategoryResponse> getAllActiveCategories() {
        log.debug("Fetching all active categories");

        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                })
                .toList();
    }

    @Override
    @Cacheable(value = "categories-list", key = "'all_featured_categories'")
    public List<CategoryResponse> getAllFeaturedCategories() {
        log.debug("Fetching all featured categories");

        return categoryRepository.findByFeaturedTrueAndIsActiveTrueOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                })
                .toList();
    }

    @Override
    @Cacheable(value = "categories-paged", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        log.debug("Fetching all categories with pagination");

        return categoryRepository.findAll(pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-paged", key = "'active:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> getActiveCategories(Pageable pageable) {
        log.debug("Fetching active categories with pagination");

        return categoryRepository.findByIsActiveTrue(pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-search", key = "'search:' + #keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> searchCategories(String keyword, Pageable pageable) {
        log.debug("Searching categories with keyword: {}", keyword);

        return categoryRepository.searchByKeyword(keyword, pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-search", key = "'active_search:' + #keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> searchActiveCategories(String keyword, Pageable pageable) {
        log.debug("Searching active categories with keyword: {}", keyword);

        return categoryRepository.searchActiveByKeyword(keyword, pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-filter",
            key = "'filter:' + T(org.springframework.util.DigestUtils).md5DigestAsHex((#name + ':' + #isActive + ':' + #featured + ':' + #hasProducts).getBytes()) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> filterCategories(
            String name,
            Boolean isActive,
            Boolean featured,
            Boolean hasProducts,
            Pageable pageable) {

        log.debug("Filtering categories - name: {}, active: {}, featured: {}, hasProducts: {}",
                name, isActive, featured, hasProducts);

        return categoryRepository.findByFilters(name, isActive, featured, hasProducts, pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-filter",
            key = "'advanced_filter:' + T(org.springframework.util.DigestUtils).md5DigestAsHex((#name + ':' + #description + ':' + #isActive + ':' + #featured + ':' + #minOrder + ':' + #maxOrder).getBytes()) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> advancedFilterCategories(
            String name,
            String description,
            Boolean isActive,
            Boolean featured,
            Integer minOrder,
            Integer maxOrder,
            Pageable pageable) {

        log.debug("Advanced filtering categories");

        return categoryRepository.findByAdvancedFilters(
                        name, description, isActive, featured, minOrder, maxOrder, pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-paged", key = "'with_products:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> getCategoriesWithProducts(Pageable pageable) {
        log.debug("Fetching categories with products");

        return categoryRepository.findCategoriesWithProducts(pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-paged", key = "'by_product_count:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> getCategoriesByProductCount(Pageable pageable) {
        log.debug("Fetching categories sorted by product count");

        return categoryRepository.findCategoriesByProductCount(pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-paged",
            key = "'created_between:' + T(org.springframework.util.DigestUtils).md5DigestAsHex((#startDate.toString() + ':' + #endDate.toString()).getBytes()) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> getCategoriesCreatedBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.debug("Fetching categories created between {} and {}", startDate, endDate);

        return categoryRepository.findByCreatedAtBetween(startDate, endDate, pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-paged",
            key = "'updated_between:' + T(org.springframework.util.DigestUtils).md5DigestAsHex((#startDate.toString() + ':' + #endDate.toString()).getBytes()) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CategoryResponse> getCategoriesUpdatedBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.debug("Fetching categories updated between {} and {}", startDate, endDate);

        return categoryRepository.findByUpdatedAtBetween(startDate, endDate, pageable)
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                });
    }

    @Override
    @Cacheable(value = "categories-list", key = "'by_ids:' + T(org.springframework.util.DigestUtils).md5DigestAsHex(#ids.toString().getBytes())")
    public List<CategoryResponse> getCategoriesByIds(List<Long> ids) {
        log.debug("Fetching categories by IDs: {}", ids);

        return categoryRepository.findByIdIn(ids)
                .stream()
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                })
                .toList();
    }

    @Override
    @Cacheable(value = "categories-list", key = "'by_slugs:' + T(org.springframework.util.DigestUtils).md5DigestAsHex(#slugs.toString().getBytes())")
    public List<CategoryResponse> getCategoriesBySlugs(List<String> slugs) {
        log.debug("Fetching categories by slugs: {}", slugs);

        return categoryRepository.findBySlugIn(slugs)
                .stream()
                .map(category -> {
                    CategoryResponse response = categoryMapper.toSimpleResponse(category);
                    response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
                    return response;
                })
                .toList();
    }

    @Override
    @Cacheable(value = "categories-stats", key = "'active_counts'")
    public long getActiveCategoryCount() {
        log.debug("Fetching active category count");
        return categoryRepository.countByIsActiveTrue();
    }


    public CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = categoryMapper.toSimpleResponse(category);
        response.setProductCount(categoryRepository.countActiveProductsInCategory(category.getId()));
        return response;
    }

    // Helper methods

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private String generateUniqueSlug(String baseSlug) {
        String uniqueSlug = baseSlug;
        int counter = 1;

        while (categoryRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = baseSlug + "-" + counter;
            counter++;
        }

        return uniqueSlug;
    }
}