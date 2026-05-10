package com.waynehays.cloudfilestorage.infrastructure.storage.exception;

public class ResourceStorageTransientException extends ResourceStorageException {

    public ResourceStorageTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
