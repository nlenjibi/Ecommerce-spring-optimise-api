package com.smart_ecomernce_api.smart_ecomernce_api.graphql.resolver;

import com.querydsl.core.types.Predicate;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.CategoryPredicates;
import com.smart_ecomernce_api.smart_ecomernce_api.common.response.PaginatedResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.dto.CategoryResponseDto;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.CategoryFilterInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.PageInput;
import com.smart_ecomernce_api.smart_ecomernce_api.graphql.input.SortDirection;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CategoryResolver {

    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;

    // ==================== Enhanced Filter Queries ====================

    @QueryMapping
    
    public CategoryResponseDto filterCategoriesWithPredicate(
            @Argument CategoryFilterInput filter,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: filterCategoriesWithPredicate with filter: {}", filter);
        Pageable pageable = createPageable(pagination);

        // Build predicate using CategoryPredicates
        Predicate predicate = CategoryPredicates.builder()
                .withNameContaining(filter.getName())
                .withDescriptionContaining(filter.getDescription())
                .withActive(filter.getIsActive())
                .withFeatured(filter.getFeatured())
                .withProducts(filter.getHasProducts())
                .withSortOrderBetween(filter.getMinOrder(), filter.getMaxOrder())
                .build();

        Page<Category> filteredCategories = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = filteredCategories.map(categoryService::mapToResponse);

        return CategoryResponseDto.builder()
                .content(responsePage.getContent())
                .pageInfo(PaginatedResponse.from(responsePage))
                .build();
    }

    @QueryMapping
    
    public CategoryResponseDto searchCategoriesWithPredicate(
            @Argument String keyword,
            @Argument CategoryFilterInput filter,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: searchCategoriesWithPredicate keyword: {}, filter: {}", keyword, filter);
        Pageable pageable = createPageable(pagination);

        // Use the complex search predicate
        Predicate predicate = CategoryPredicates.builder()
                .withSearch(keyword, filter != null ? filter.getIsActive() : null,
                        filter != null ? filter.getFeatured() : null,
                        filter != null ? filter.getHasProducts() : null)
                .build();

        Page<Category> searchResults = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = searchResults.map(categoryService::mapToResponse);

        return CategoryResponseDto.builder()
                .content(responsePage.getContent())
                .pageInfo(PaginatedResponse.from(responsePage))
                .build();
    }

    @QueryMapping
    
    public CategoryResponseDto getRootCategories(
            @Argument Boolean featured,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: getRootCategories featured: {}", featured);
        Pageable pageable = createPageable(pagination);

        Predicate predicate = CategoryPredicates.builder()
                .withRootCategory(true)
                .withActive(true)
                .withFeatured(featured)
                .build();

        Page<Category> rootCategories = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = rootCategories.map(categoryService::mapToResponse);

        return CategoryResponseDto.builder()
                .content(responsePage.getContent())
                .pageInfo(PaginatedResponse.from(responsePage))
                .build();
    }

    @QueryMapping
    
    public CategoryResponseDto getCategoriesWithMinProductCount(
            @Argument Integer minProductCount,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: getCategoriesWithMinProductCount minCount: {}", minProductCount);
        Pageable pageable = createPageable(pagination);

        Predicate predicate = CategoryPredicates.builder()
                .withActive(true)
                .withMinProductCount(minProductCount)
                .build();

        Page<Category> categories = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = categories.map(categoryService::mapToResponse);

        return CategoryResponseDto.builder()
                .content(responsePage.getContent())
                .pageInfo(PaginatedResponse.from(responsePage))
                .build();
    }

    @QueryMapping
    
    public CategoryResponseDto getCategoriesByDateRange(
            @Argument LocalDateTime startDate,
            @Argument LocalDateTime endDate,
            @Argument String type, // "created" or "updated"
            @Argument PageInput pagination) {
        log.info("GraphQL Query: getCategoriesByDateRange type: {}, from {} to {}", type, startDate, endDate);
        Pageable pageable = createPageable(pagination);

        Predicate predicate;
        if ("created".equalsIgnoreCase(type)) {
            predicate = CategoryPredicates.builder()
                    .withCreatedBetween(startDate, endDate)
                    .build();
        } else {
            predicate = CategoryPredicates.builder()
                    .build(); // UpdatedAt filtering would need custom implementation
        }

        Page<Category> categories = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = categories.map(categoryService::mapToResponse);

        return CategoryResponseDto.builder()
                .content(responsePage.getContent())
                .pageInfo(PaginatedResponse.from(responsePage))
                .build();
    }

    @QueryMapping
    
    public List<CategoryResponse> getFeaturedRootCategories() {
        log.info("GraphQL Query: getFeaturedRootCategories");

        Predicate predicate = CategoryPredicates.builder()
                .withFeaturedRootCategory()
                .build();

        List<Category> categories = (List<Category>) categoryRepository.findAll(predicate);
        return categories.stream()
                .map(categoryService::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @QueryMapping
    
    public CategoryResponseDto getCategoriesBySubcategoryPresence(
            @Argument Boolean hasSubcategories,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: getCategoriesBySubcategoryPresence: {}", hasSubcategories);
        Pageable pageable = createPageable(pagination);

        Predicate predicate = CategoryPredicates.builder()
                .withActive(true)
                .withSubcategories(hasSubcategories)
                .build();

        Page<Category> categories = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = categories.map(categoryService::mapToResponse);

        return CategoryResponseDto.builder()
                .content(responsePage.getContent())
                .pageInfo(PaginatedResponse.from(responsePage))
                .build();
    }

    // ==================== Existing Queries (Enhanced with predicates) ====================

    @QueryMapping
    
    public CategoryResponseDto categories(@Argument PageInput pagination) {
        log.info("GraphQL Query: categories with pagination");
        Pageable pageable = createPageable(pagination);
        Page<CategoryResponse> allCategories = categoryService.getAllCategories(pageable);

        return CategoryResponseDto.builder()
                .content(allCategories.getContent())
                .pageInfo(PaginatedResponse.from(allCategories))
                .build();
    }

    @QueryMapping
    
    public CategoryResponseDto advancedFilterCategories(
            @Argument CategoryFilterInput filter,
            @Argument PageInput pagination) {
        log.info("GraphQL Query: advancedFilterCategories with filter: {}", filter);
        Pageable pageable = createPageable(pagination);

        // Enhanced with predicate-based filtering
        Predicate predicate = CategoryPredicates.builder()
                .withNameContaining(filter.getName())
                .withDescriptionContaining(filter.getDescription())
                .withActive(filter.getIsActive())
                .withFeatured(filter.getFeatured())
                .withSortOrderBetween(filter.getMinOrder(), filter.getMaxOrder())
                .build();

        Page<Category> filteredCategories = categoryRepository.findAll(predicate, pageable);
        Page<CategoryResponse> responsePage = filteredCategories.map(categoryService::mapToResponse);

        return CategoryResponseDto.builder()
                .content(responsePage.getContent())
                .pageInfo(PaginatedResponse.from(responsePage))
                .build();
    }

    // ==================== Keep existing mutations and basic queries ====================

    // ... existing mutations and queries remain the same ...

    @QueryMapping
    
    public CategoryResponse category(@Argument Long id) {
        log.info("GraphQL Query: category(id: {})", id);
        return categoryService.getCategoryById(id);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public CategoryResponse createCategory(@Argument CategoryCreateRequest input) {
        log.info("GraphQL Mutation: createCategory(name: {})", input.getName());
        return categoryService.createCategory(input);
    }

    // ==================== Helper Methods ====================

    private Pageable createPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "displayOrder"));
        }

        String sortBy = input.getSortBy() != null ? input.getSortBy() : "displayOrder";
        Sort.Direction direction = input.getDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, sortBy);
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}
