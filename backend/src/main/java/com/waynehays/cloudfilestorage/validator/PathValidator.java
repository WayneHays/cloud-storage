package com.waynehays.cloudfilestorage.validator;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import com.waynehays.cloudfilestorage.config.properties.PathLimitsProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class PathValidator implements ConstraintValidator<ValidPath, String> {
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[\\p{L}\\p{N}._\\- ]+$");
    private static final String SEPARATOR = "/";
    private static final String CURRENT_DIR = ".";
    private static final String PARENT_DIR = "..";

    private final PathLimitsProperties limitsProperties;
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

            if (!isValidSegment(segment)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidSegment(String segment) {
        if (segment.isBlank()) {
            return false;
        }
        if (PARENT_DIR.equals(segment) || CURRENT_DIR.equals(segment)) {
            return false;
        }
        if (segment.startsWith(CURRENT_DIR) || segment.endsWith(CURRENT_DIR)) {
            return false;
        }

        return ALLOWED_CHARACTERS.matcher(segment).matches();
    }
}
