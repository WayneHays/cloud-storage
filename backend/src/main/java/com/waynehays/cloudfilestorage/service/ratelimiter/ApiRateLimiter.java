package com.waynehays.cloudfilestorage.service.ratelimiter;

import com.waynehays.cloudfilestorage.service.ratelimiter.dto.RequestData;
import com.waynehays.cloudfilestorage.service.ratelimiter.dto.RateLimitCheckResult;
import com.waynehays.cloudfilestorage.service.ratelimiter.dto.RateLimitRule;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ApiRateLimiter {
    private final RuleRegistry ruleRegistry;
    private final BucketRegistry bucketRegistry;
    private final RateLimitCheckResultFactory resultFactory;

    public RateLimitCheckResult checkRateLimit(RequestData requestData) {
        Optional<RateLimitRule> rule = ruleRegistry.getRule(requestData.endpoint(), requestData.httpMethod());

        if (rule.isEmpty()) {
            return resultFactory.unlimited();
        }

        Bucket bucket = bucketRegistry.getOrCreateBucket(requestData, rule.get());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return resultFactory.allowed(probe.getRemainingTokens());
        }

        long waitSeconds = convertNanosToSeconds(probe.getNanosToWaitForRefill());
        return resultFactory.rejected(waitSeconds,
                "Too many requests to: " + requestData.endpoint());
    }

    private long convertNanosToSeconds(long nanoseconds) {
        return TimeUnit.NANOSECONDS.toSeconds(nanoseconds);
    }
}
