package com.smart_ecomernce_api.smart_ecomernce_api.validator;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.dto.ProductCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProductCreateRequestPriceRangeValidator implements ConstraintValidator<ValidPriceRange, ProductCreateRequest> {

    @Override
    public void initialize(ValidPriceRange annotation) {
        ConstraintValidator.super.initialize(annotation);
    }

    @Override
    public boolean isValid(ProductCreateRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getPrice() == null || value.getDiscountedPrice() == null) {
            return true; // Let @NotNull handle null validation
        }

        // Discounted price should always be less than or equal to original price
        boolean isValid = value.getDiscountedPrice().compareTo(value.getPrice()) <= 0;

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Discounted price (" + value.getDiscountedPrice() +
                            ") must be less than or equal to original price (" + value.getPrice() + ")")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
