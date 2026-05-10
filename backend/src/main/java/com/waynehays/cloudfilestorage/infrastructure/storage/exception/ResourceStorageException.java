package com.waynehays.cloudfilestorage.infrastructure.storage.exception;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;

public class ResourceStorageException extends ApplicationException {

    public ResourceStorageException(String message) {
        super(message);
    }

    public ResourceStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
