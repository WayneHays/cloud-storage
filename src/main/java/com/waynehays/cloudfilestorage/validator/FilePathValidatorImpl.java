package com.waynehays.cloudfilestorage.validator;

import com.waynehays.cloudfilestorage.constant.Constants;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.InvalidFileNameException;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;

@Component
public class FilePathValidatorImpl implements FilePathValidator {
    private static final Pattern ALLOWED_CHARACTERS_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final String MSG_NOT_BLANK = "Filename cannot be blank";
    private static final String MSG_EMPTY_PARTS = "Path contains empty parts";
    private static final String MSG_PATH_TRAVERSAL = "Path traversal detected";
    private static final String MSG_PATH_TRAVERSAL_DIRECTORY = "Path traversal detected in directory";
    private static final String MSG_INVALID_CHARACTERS = "Invalid characters in: ";
    private static final String MSG_INVALID_CHARACTERS_DIRECTORY = "Invalid characters in directory: ";
    private static final String PARENT_DIRECTORY = "..";

    public void validate(String originalFilename, String directory) {

        if (StringUtils.isBlank(originalFilename)) {
            throw new InvalidFileNameException(MSG_NOT_BLANK, originalFilename);
        }

        String normalized = FilenameUtils.separatorsToUnix(originalFilename);

        String[] parts = normalized.split(Constants.PATH_SEPARATOR);

        for (String part : parts) {
            validatePart(originalFilename, part);
        }

        if (directory != null) {
            validateDirectory(directory);
        }
    }

    private void validatePart(String originalFilename, String part) {
        if (StringUtils.isBlank(part)) {
            throw new InvalidFileNameException(MSG_EMPTY_PARTS, originalFilename);
        }

        if (PARENT_DIRECTORY.equals(part)) {
            throw new InvalidFileNameException(MSG_PATH_TRAVERSAL, originalFilename);
        }

        if (!ALLOWED_CHARACTERS_PATTERN.matcher(part).matches()) {
            throw new InvalidFileNameException(MSG_INVALID_CHARACTERS + part, originalFilename);
        }
    }

    private void validateDirectory(String directory) {
        String normalized = FilenameUtils.separatorsToUnix(directory);
        String[] parts = normalized.split(Constants.PATH_SEPARATOR);

        for (String part : parts) {
            if (StringUtils.isBlank(part)) {
                continue;
            }

            if (PARENT_DIRECTORY.equals(part)) {
                throw new InvalidPathException(MSG_PATH_TRAVERSAL_DIRECTORY, directory);
            }

            if (!ALLOWED_CHARACTERS_PATTERN.matcher(part).matches()) {
                throw new InvalidPathException(MSG_INVALID_CHARACTERS_DIRECTORY + part, directory);
            }
        }
    }
}
