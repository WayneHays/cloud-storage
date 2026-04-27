package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
class RateLimiterService implements RateLimiterServiceApi {
    private final RuleRegistry ruleRegistry;
    private final BucketRegistry bucketRegistry;

    @Override
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
        return TimeUnit.NANOSECONDS.toSeconds(nanoseconds);
    }
}
