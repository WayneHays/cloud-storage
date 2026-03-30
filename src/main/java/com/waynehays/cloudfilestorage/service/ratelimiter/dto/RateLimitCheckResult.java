package com.waynehays.cloudfilestorage.service.ratelimiter.dto;

public record RateLimitCheckResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds,
        String errorMessage
) {
    public static RateLimitCheckResult allowed(long remainingTokens) {
        return new RateLimitCheckResult(true, remainingTokens, 0, null);
    }

    public static RateLimitCheckResult rejected(long retryAfterSeconds, String message) {
        return new RateLimitCheckResult(false, 0, retryAfterSeconds, message);
    }

    public static RateLimitCheckResult unlimited() {
        return new RateLimitCheckResult(true, -1, 0, null);
    }
}
