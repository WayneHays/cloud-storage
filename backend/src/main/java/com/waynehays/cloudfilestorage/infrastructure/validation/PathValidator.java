package com.waynehays.cloudfilestorage.infrastructure.validation;

import com.waynehays.cloudfilestorage.core.utils.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class PathValidator implements ConstraintValidator<ValidPath, String> {
    private static final String SEPARATOR = "/";

    private final ResourceLimitsProperties limitsProperties;
    private boolean mustBeDirectory;

    @Override
    public void initialize(ValidPath annotation) {
        this.mustBeDirectory = annotation.mustBeDirectory();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }

        if (value.length() > limitsProperties.maxPathLength()) {
            return false;
        }

        if (mustBeDirectory && !value.endsWith(SEPARATOR)) {
            return false;
        }

        String[] segments = value.split(SEPARATOR, -1);

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean isLast = (i == segments.length - 1);

            if (isLast && segment.isEmpty()) {
                continue;
            }

            if (ValidationUtils.isInvalidInput(segment)) {
                return false;
            }
        }

        return true;
    }
}
