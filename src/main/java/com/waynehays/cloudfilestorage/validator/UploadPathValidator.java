package com.waynehays.cloudfilestorage.validator;

public interface UploadPathValidator {

    void validate(String originalFilename, String directory);
}
