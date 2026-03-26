package com.waynehays.cloudfilestorage.annotation;

import com.waynehays.cloudfilestorage.component.validator.PathNotEqualsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PathNotEqualsValidator.class)
public @interface PathNotEquals {
    String message() default "Source and target paths cannot be equal";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
