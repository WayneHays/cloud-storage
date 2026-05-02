package com.waynehays.cloudfilestorage.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PathValidator.class)
public @interface ValidPath {
    String message() default "Invalid path";
    boolean mustBeDirectory() default false;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
