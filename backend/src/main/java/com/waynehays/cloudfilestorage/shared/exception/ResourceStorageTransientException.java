package com.waynehays.cloudfilestorage.shared.exception;

public class ResourceStorageTransientException extends ResourceStorageOperationException {

    public ResourceStorageTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
