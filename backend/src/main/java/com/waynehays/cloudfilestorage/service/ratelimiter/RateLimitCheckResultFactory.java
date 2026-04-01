package com.waynehays.cloudfilestorage.service.ratelimiter;

import com.waynehays.cloudfilestorage.service.ratelimiter.dto.RateLimitCheckResult;
import org.springframework.stereotype.Component;

@Component
public class RateLimitCheckResultFactory {

    public RateLimitCheckResult allowed(long remainingTokens) {
        return new RateLimitCheckResult(true, remainingTokens, 0, null);
    }

    public RateLimitCheckResult rejected(long retryAfterSeconds, String message) {
        return new RateLimitCheckResult(false, 0, retryAfterSeconds, message);
    }

    public RateLimitCheckResult unlimited() {
        return new RateLimitCheckResult(true, -1, 0, null);
    }
}
