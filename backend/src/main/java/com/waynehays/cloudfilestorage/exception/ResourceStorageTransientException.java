package com.waynehays.cloudfilestorage.exception;

public class ResourceStorageTransientException extends ResourceStorageOperationException {

    public ResourceStorageTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
