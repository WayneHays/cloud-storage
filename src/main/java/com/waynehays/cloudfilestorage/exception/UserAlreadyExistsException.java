package com.waynehays.cloudfilestorage.exception;

public class UserAlreadyExistsException extends ApplicationException {

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
