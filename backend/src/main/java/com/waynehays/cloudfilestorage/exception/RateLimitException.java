package com.waynehays.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class RateLimitException extends ApplicationException {
    private final String endpoint;
    private final String httpMethod;
    private final long retryAfter;

    public RateLimitException(String message, String endpoint, String httpMethod ,long retryAfter) {
        super(message);
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.retryAfter = retryAfter;
    }
}
