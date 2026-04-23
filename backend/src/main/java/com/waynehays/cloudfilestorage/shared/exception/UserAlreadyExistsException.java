package com.waynehays.cloudfilestorage.shared.exception;

public class UserAlreadyExistsException extends ApplicationException {

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
