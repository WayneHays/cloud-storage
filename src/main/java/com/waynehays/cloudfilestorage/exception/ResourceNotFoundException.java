package com.waynehays.cloudfilestorage.exception;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
