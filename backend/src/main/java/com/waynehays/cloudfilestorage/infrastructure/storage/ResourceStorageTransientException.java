package com.waynehays.cloudfilestorage.infrastructure.storage;

public class ResourceStorageTransientException extends ResourceStorageOperationException {

    public ResourceStorageTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
