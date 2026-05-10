package com.waynehays.cloudfilestorage.core.quota.exception;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;
import lombok.Getter;

@Getter
public class QuotaNotFoundException extends ApplicationException {
    private final Long userId;

    public QuotaNotFoundException(Long userId) {
        this.userId = userId;
    }
}
