package com.waynehays.cloudfilestorage.exception;

public class UserAlreadyExistsException extends CloudFileStorageException {
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
