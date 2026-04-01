package com.waynehays.cloudfilestorage.service.ratelimiter.dto;

public record RateLimitCheckResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds,
        String errorMessage
) {
}
