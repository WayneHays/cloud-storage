package com.waynehays.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class UserNotFoundException extends ApplicationException {
    private final Long userId;

    public UserNotFoundException(String message, Long userId) {
        super(message);
        this.userId = userId;
    }
}
