package com.waynehays.cloudfilestorage.component.validator;

import com.waynehays.cloudfilestorage.config.properties.MultipartFileLimitsProperties;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultipartFileValidator {
    private final MultipartFileLimitsProperties properties;

    public void validate(String originalFilename, String fullPath) {

        if (StringUtils.isBlank(originalFilename)) {
            throwException("Uploaded file has no filename");
        }

        if (originalFilename.length() > properties.maxPathLength()) {
            throwException("Original filename exceeds max length of " + properties.maxPathLength() + " characters");
        }

        String filename = PathUtils.extractFilename(originalFilename);

        if (filename.length() > properties.maxFilenameLength()) {
            throwException("Filename exceeds max length of " + properties.maxFilenameLength() + " characters");
        }

        if (fullPath.length() > properties.maxPathLength()) {
            throwException("Full path exceeds maximum length of " + properties.maxPathLength() + " characters");
        }
    }

    private void throwException(String message) {
        throw new MultipartValidationException(message);
    }
}
