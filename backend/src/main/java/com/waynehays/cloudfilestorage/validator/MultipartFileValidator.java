package com.waynehays.cloudfilestorage.validator;

import com.waynehays.cloudfilestorage.config.properties.ResourceLimitsProperties;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import com.waynehays.cloudfilestorage.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultipartFileValidator {
    private final ResourceLimitsProperties properties;

    public void validate(String originalFilename, String fullPath, long fileSize) {
        if (StringUtils.isBlank(originalFilename)) {
            throw new MultipartValidationException("Uploaded file has no filename");
        }

        String filename = PathUtils.extractFilename(originalFilename);

        if (ValidationUtils.isInvalidInput(filename)) {
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

        if (fileSize > properties.maxFileSize().toBytes()) {
            throw new MultipartValidationException(
                    "File '%s' exceeds max size of %s".formatted(filename, properties.maxFileSize()));
        }
    }
}
