package com.waynehays.cloudfilestorage.validator;

import com.waynehays.cloudfilestorage.config.properties.PathLimitsProperties;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import com.waynehays.cloudfilestorage.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultipartFileValidator {
    private final PathLimitsProperties properties;

    public void validate(String originalFilename, String fullPath) {
        if (StringUtils.isBlank(originalFilename)) {
            throw new MultipartValidationException("Uploaded file has no filename");
        }

        String filename = PathUtils.extractFilename(originalFilename);

        if (ValidationUtils.isInvalidSegment(filename)) {
            throw new MultipartValidationException("Filename contains invalid characters");
        }

        if (filename.length() > properties.maxFilenameLength()) {
            throw new MultipartValidationException(
                    "Filename exceeds max length of %d characters".formatted(properties.maxFilenameLength()));
        }

        if (fullPath.length() > properties.maxPathLength()) {
            throw new MultipartValidationException(
                    "Full path exceeds max length of %d characters".formatted(properties.maxPathLength()));
        }
    }
}
