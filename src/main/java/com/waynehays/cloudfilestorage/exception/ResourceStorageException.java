package com.waynehays.cloudfilestorage.exception;

public class ResourceStorageException extends ApplicationException {

    public ResourceStorageException(String message) {
        super(message);
    }

    public ResourceStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
