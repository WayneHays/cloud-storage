package com.waynehays.cloudfilestorage.core.metadata.exception;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends ApplicationException {
    private final String path;

    public ResourceNotFoundException(String message, String path) {
        super(message);
        this.path = path;
    }
}
