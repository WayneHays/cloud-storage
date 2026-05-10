package com.waynehays.cloudfilestorage.core.quota.exception;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;
import lombok.Getter;

@Getter
public class QuotaLimitException extends ApplicationException {
    private final Long uploadSize;
    private final Long freeSpace;

    public QuotaLimitException(Long uploadSize, Long freeSpace) {
        this.uploadSize = uploadSize;
        this.freeSpace = freeSpace;
    }
}
