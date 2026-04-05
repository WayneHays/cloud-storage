package com.waynehays.cloudfilestorage.ratelimiter.dto;

public record RateLimitCheckResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds,
        String errorMessage
) {

    public boolean hasRemainingTokens() {
        return remainingTokens >= 0;
    }
}
