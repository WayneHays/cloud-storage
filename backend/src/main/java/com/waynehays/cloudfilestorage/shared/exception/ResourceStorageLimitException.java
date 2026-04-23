package com.waynehays.cloudfilestorage.shared.exception;

import lombok.Getter;

@Getter
public class ResourceStorageLimitException extends ApplicationException {
    private final Long uploadSize;
    private final Long freeSpace;

    public ResourceStorageLimitException(String message, Long uploadSize, Long freeSpace) {
        super(message);
        this.uploadSize = uploadSize;
        this.freeSpace = freeSpace;
    }
}
