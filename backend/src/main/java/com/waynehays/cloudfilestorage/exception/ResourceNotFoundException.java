package com.waynehays.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends ApplicationException {
    private final String path;

    public ResourceNotFoundException(String message, String path) {
        super(message);
        this.path = path;
    }
}
