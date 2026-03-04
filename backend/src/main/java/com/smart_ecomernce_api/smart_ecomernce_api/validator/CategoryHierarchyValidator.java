package com.smart_ecomernce_api.smart_ecomernce_api.validator;


import com.smart_ecomernce_api.smart_ecomernce_api.modules.category.repository.CategoryRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

public class CategoryHierarchyValidator implements ConstraintValidator<ValidCategoryHierarchy, Long> {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void initialize(ValidCategoryHierarchy annotation) {
        ConstraintValidator.super.initialize(annotation);
    }

    @Override
    public boolean isValid(Long parentId, ConstraintValidatorContext context) {
        if (parentId == null) {
            return true; // No parent is valid
        }

        // Verify parent category exists and is active
        return categoryRepository.findById(parentId)
                .map(category -> category.getIsActive())
                .orElse(false);
    }
}