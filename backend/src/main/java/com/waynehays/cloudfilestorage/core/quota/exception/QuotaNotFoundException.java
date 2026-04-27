package com.waynehays.cloudfilestorage.core.quota.exception;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;
import lombok.Getter;

@Getter
public class QuotaNotFoundException extends ApplicationException {
    private final Long userId;

    public QuotaNotFoundException(String message, Long userId) {
        super(message);
        this.userId = userId;
    }
}
