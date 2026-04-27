package com.waynehays.cloudfilestorage.files.api.resource;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;

public class UploadValidationException extends ApplicationException {

    UploadValidationException(String message) {
        super(message);
    }
}
