package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class BucketRegistry {
    private final Cache<RequestData, Bucket> buckets;

    BucketRegistry(RateLimitProperties properties) {
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(properties.bucketExpiration())
                .build();
    }

    Bucket getOrCreateBucket(RequestData data, RateLimitRule rule) {
        return buckets.get(data, key -> Bucket.builder()
                .addLimit(limit -> limit.capacity(rule.capacity())
                        .refillGreedy(rule.tokensPerMinute(), Duration.ofMinutes(1)))
                .build());
    }
}
