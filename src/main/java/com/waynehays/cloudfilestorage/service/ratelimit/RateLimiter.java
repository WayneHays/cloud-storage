package com.waynehays.cloudfilestorage.service.ratelimit;

import com.waynehays.cloudfilestorage.service.ratelimit.dto.RequestData;
import com.waynehays.cloudfilestorage.service.ratelimit.dto.RateLimitCheckResult;
import com.waynehays.cloudfilestorage.service.ratelimit.dto.RateLimitRule;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RateLimiter {
    private final RuleRegistry ruleRegistry;
    private final BucketRegistry bucketRegistry;

    public RateLimitCheckResult checkRateLimit(RequestData requestData) {
        Optional<RateLimitRule> rule = ruleRegistry.getRule(requestData.endpoint(), requestData.httpMethod());

        if (rule.isEmpty()) {
            return RateLimitCheckResult.unlimited();
        }

        Bucket bucket = bucketRegistry.getOrCreateBucket(requestData, rule.get());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitCheckResult.allowed(probe.getRemainingTokens());
        }

        long waitSeconds = convertNanosToSeconds(probe.getNanosToWaitForRefill());
        return RateLimitCheckResult.rejected(waitSeconds,
                "Too many requests to: " + requestData.endpoint());
    }

    private long convertNanosToSeconds(long nanoseconds) {
        return nanoseconds / 1_000_000_000;
    }
}
