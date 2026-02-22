package com.waynehays.cloudfilestorage.validator;

import com.waynehays.cloudfilestorage.exception.InvalidFilenameException;
import com.waynehays.cloudfilestorage.exception.InvalidPathException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PathValidatorImpl implements PathValidator {
    private static final Pattern ALLOWED_CHARACTERS_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final String MSG_BLANK_FILENAME = "Filename cannot be blank: ";
    private static final String MSG_INVALID_PATH_PART = "Invalid path part: ";
    private static final String MSG_PATH_TRAVERSAL = "Path traversal detected: ";
    private static final String MSG_INVALID_CHARACTERS = "Invalid characters in path: ";
    private static final String PARENT_DIRECTORY = "..";

    public void validateUploadPath(String originalFilename, String directoryPath) {
        validateFilename(originalFilename);

        if (directoryPath != null) {
            validateDirectoryPath(directoryPath);
        }
    }

    public void validateDirectoryPath(String directoryPath) {
        if (StringUtils.isBlank(directoryPath)) {
            return;
        }

        String normalizedPath = PathUtils.normalize(directoryPath);

        if (normalizedPath.isEmpty()) {
            return;
        }

        for (String part : PathUtils.splitIntoParts(normalizedPath)) {
            if (StringUtils.isNotBlank(part)) {
                validatePathPart(part);
            }
        }
    }

    private void validateFilename(String filename) {
        if (StringUtils.isBlank(filename)) {
            throw new InvalidFilenameException(MSG_BLANK_FILENAME + filename);
        }

        try {
            for (String part : PathUtils.splitIntoParts(PathUtils.normalizeSeparators(filename))) {
                validatePathPart(part);
            }
        } catch (InvalidPathException e) {
            throw new InvalidFilenameException(e.getMessage() + filename);
        }
    }

    private void validatePathPart(String part) {
        if (StringUtils.isBlank(part)) {
            throw new InvalidPathException(MSG_INVALID_PATH_PART + part);
        }

        if (isParentDirectoryReference(part)) {
            throw new InvalidPathException(MSG_PATH_TRAVERSAL + part);
        }

        if (!containsOnlyAllowedCharacters(part)) {
            throw new InvalidPathException(MSG_INVALID_CHARACTERS + part);
        }
    }

    private boolean isParentDirectoryReference(String part) {
        return PARENT_DIRECTORY.equals(part);
    }

    private boolean containsOnlyAllowedCharacters(String part) {
        return ALLOWED_CHARACTERS_PATTERN.matcher(part).matches();
    }
}
