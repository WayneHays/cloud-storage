package com.waynehays.cloudfilestorage.exception;

public class ResourceStorageOperationException extends ApplicationException {

    public ResourceStorageOperationException(String message) {
        super(message);
    }

    public ResourceStorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
