package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

record RateLimitCheckResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds
) {

    boolean isRateLimited() {
        return remainingTokens >= 0;
    }

    static RateLimitCheckResult allowed(long remainingTokens) {
        return new RateLimitCheckResult(true, remainingTokens, 0);
    }

    static RateLimitCheckResult rejected(long retryAfterSeconds) {
        return new RateLimitCheckResult(false, 0, retryAfterSeconds);
    }

    static RateLimitCheckResult unlimited() {
        return new RateLimitCheckResult(true, -1, 0);
    }
}
