package com.waynehays.cloudfilestorage.ratelimiter;

import com.waynehays.cloudfilestorage.ratelimiter.dto.RateLimitCheckResult;
import org.springframework.stereotype.Component;

@Component
public class RateLimitCheckResultFactory {

    public RateLimitCheckResult allowed(long remainingTokens) {
        return createCheckResult(true, remainingTokens, 0, null);
    }

    public RateLimitCheckResult rejected(long retryAfterSeconds, String message) {
        return createCheckResult(false, 0, retryAfterSeconds, message);
    }

    public RateLimitCheckResult unlimited() {
        return createCheckResult(true, -1, 0, null);
    }

    private RateLimitCheckResult createCheckResult(boolean allowed, long remainingTokens,
                                                   long retryAfterSeconds, String errorMessage) {
        return new RateLimitCheckResult(allowed, remainingTokens, retryAfterSeconds, errorMessage);
    }
}
