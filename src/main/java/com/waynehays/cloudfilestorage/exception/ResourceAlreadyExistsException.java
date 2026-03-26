package com.waynehays.cloudfilestorage.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ResourceAlreadyExistsException extends ApplicationException {
    private final List<String> paths;

    public ResourceAlreadyExistsException(String message, String path) {
        super(message);
        this.paths = List.of(path);
    }

    public ResourceAlreadyExistsException(String message, List<String> paths) {
        super(message);
        this.paths = paths;
    }
}
