package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;
import lombok.Getter;

@Getter
public class RateLimitException extends ApplicationException {
    private final String endpoint;
    private final String httpMethod;
    private final long retryAfter;

    RateLimitException(String endpoint, String httpMethod, long retryAfter) {
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.retryAfter = retryAfter;
    }
}
