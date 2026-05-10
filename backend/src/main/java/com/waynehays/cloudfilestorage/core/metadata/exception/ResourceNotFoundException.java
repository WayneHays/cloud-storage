package com.waynehays.cloudfilestorage.core.metadata.exception;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends ApplicationException {
    private final String path;

    public ResourceNotFoundException(String message, String path) {
        super(message);
        this.path = path;
    }
}
