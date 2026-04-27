package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;

public class UserAlreadyExistsException extends ApplicationException {

    UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
