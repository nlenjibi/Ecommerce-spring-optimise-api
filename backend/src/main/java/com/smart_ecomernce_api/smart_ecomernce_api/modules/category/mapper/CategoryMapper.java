package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.mapper;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryCreateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto.CategoryUpdateRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface CategoryMapper {

    /**
     * Convert create request to entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "featured", expression = "java(request.getFeatured() != null ? request.getFeatured() : false)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryCreateRequest request);

    /**
     * Convert entity to response
     */
    @Mapping(target = "productCount", ignore = true)
    CategoryResponse toSimpleResponse(Category category);

    /**
     * Update entity from request (partial update)
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CategoryUpdateRequest request, @MappingTarget Category category);
}