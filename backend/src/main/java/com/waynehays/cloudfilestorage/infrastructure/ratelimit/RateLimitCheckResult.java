package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

record RateLimitCheckResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds,
        String errorMessage
) {

    boolean isRateLimited() {
        return remainingTokens >= 0;
    }

    static RateLimitCheckResult allowed(long remainingTokens) {
        return new RateLimitCheckResult(true, remainingTokens, 0, null);
    }

    static RateLimitCheckResult rejected(long retryAfterSeconds, String message) {
        return new RateLimitCheckResult(false, 0, retryAfterSeconds, message);
    }

    static RateLimitCheckResult unlimited() {
        return new RateLimitCheckResult(true, -1, 0, null);
    }
}
