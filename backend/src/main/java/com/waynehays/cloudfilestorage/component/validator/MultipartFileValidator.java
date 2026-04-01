package com.waynehays.cloudfilestorage.component.validator;

import com.waynehays.cloudfilestorage.config.properties.PathLimitsProperties;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MultipartFileValidator {
    private static final String MSG_TEMPLATE = "%s exceeds max length of %d characters";

    private final PathLimitsProperties properties;

    public void validate(String originalFilename, String fullPath) {

        if (StringUtils.isBlank(originalFilename)) {
            throwException("Uploaded file has no filename");
        }

        if (originalFilename.length() > properties.maxPathLength()) {
            throwException(MSG_TEMPLATE.formatted("Original filename", properties.maxPathLength()));
        }

        String filename = PathUtils.extractFilename(originalFilename);

        if (filename.length() > properties.maxFilenameLength()) {
            throwException(MSG_TEMPLATE.formatted("Filename", properties.maxFilenameLength()));
        }

        if (fullPath.length() > properties.maxPathLength()) {
            throwException(MSG_TEMPLATE.formatted("Full path", properties.maxPathLength()));
        }
    }

    private void throwException(String message) {
        throw new MultipartValidationException(message);
    }
}
