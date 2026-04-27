package com.waynehays.cloudfilestorage.infrastructure.storage;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;

public class ResourceStorageException extends ApplicationException {

    public ResourceStorageException(String message) {
        super(message);
    }

    public ResourceStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
