package com.smart_ecomernce_api.smart_ecomernce_api.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CategoryHierarchyValidator.class)
@Documented
public @interface ValidCategoryHierarchy {
    String message() default "Invalid category hierarchy";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}