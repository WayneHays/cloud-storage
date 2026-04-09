package com.waynehays.cloudfilestorage.ratelimiter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waynehays.cloudfilestorage.config.properties.RateLimitProperties;
import com.waynehays.cloudfilestorage.dto.internal.ratelimit.RateLimitRule;
import com.waynehays.cloudfilestorage.dto.internal.ratelimit.RequestData;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BucketRegistry {
    private final Cache<RequestData, Bucket> buckets;

    public BucketRegistry(RateLimitProperties properties) {
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(properties.bucketExpiration())
                .build();
    }

    public Bucket getOrCreateBucket(RequestData data, RateLimitRule rule) {
        return buckets.get(data, key -> Bucket.builder()
                .addLimit(limit -> limit.capacity(rule.capacity())
                        .refillGreedy(rule.tokensPerMinute(), Duration.ofMinutes(1)))
                .build());
    }
}
