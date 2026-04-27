package com.waynehays.cloudfilestorage.core.quota.exception;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;
import lombok.Getter;

@Getter
public class QuotaLimitException extends ApplicationException {
    private final Long uploadSize;
    private final Long freeSpace;

    public QuotaLimitException(String message, Long uploadSize, Long freeSpace) {
        super(message);
        this.uploadSize = uploadSize;
        this.freeSpace = freeSpace;
    }
}
