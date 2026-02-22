package com.waynehays.cloudfilestorage.validator;

public interface PathValidator {

    void validateUploadPath(String originalFilename, String directory);

    void validateDirectoryPath(String queryPath);
}
