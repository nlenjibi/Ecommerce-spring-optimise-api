package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateRequest {

    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Min(value = 0, message = "Display order must be non-negative")
    private Integer displayOrder;

    private Boolean featured;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

}