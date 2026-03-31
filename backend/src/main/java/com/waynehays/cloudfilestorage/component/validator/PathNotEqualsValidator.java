package com.waynehays.cloudfilestorage.component.validator;

import com.waynehays.cloudfilestorage.annotation.PathNotEquals;
import com.waynehays.cloudfilestorage.dto.request.resource.MoveRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PathNotEqualsValidator implements ConstraintValidator<PathNotEquals, MoveRequest> {

    @Override
    public boolean isValid(MoveRequest request, ConstraintValidatorContext context) {
        return !request.from().equalsIgnoreCase(request.to());
    }
}
