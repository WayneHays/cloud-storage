package com.waynehays.cloudfilestorage.infrastructure.storage;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;

public class ResourceStorageOperationException extends ApplicationException {

    public ResourceStorageOperationException(String message) {
        super(message);
    }

    public ResourceStorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
