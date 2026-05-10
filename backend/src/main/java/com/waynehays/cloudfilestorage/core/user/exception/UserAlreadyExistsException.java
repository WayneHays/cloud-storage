package com.waynehays.cloudfilestorage.core.user.exception;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;

public class UserAlreadyExistsException extends ApplicationException {

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
