package com.waynehays.cloudfilestorage.validator;

public interface FilePathValidator {

    void validate(String originalFilename, String directory);
}
