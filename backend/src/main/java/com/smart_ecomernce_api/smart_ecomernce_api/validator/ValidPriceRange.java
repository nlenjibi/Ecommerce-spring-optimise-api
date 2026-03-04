package com.smart_ecomernce_api.smart_ecomernce_api.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {PriceRangeValidator.class, ProductCreateRequestPriceRangeValidator.class})
@Documented
public @interface ValidPriceRange {
    String message() default "Discounted price must be less than original price";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}