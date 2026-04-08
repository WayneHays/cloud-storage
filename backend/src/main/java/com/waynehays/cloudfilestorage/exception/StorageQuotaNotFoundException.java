package com.waynehays.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class StorageQuotaNotFoundException extends ApplicationException {
    private final Long userId;

    public StorageQuotaNotFoundException(String message, Long userId) {
        super(message);
        this.userId = userId;
    }
}
