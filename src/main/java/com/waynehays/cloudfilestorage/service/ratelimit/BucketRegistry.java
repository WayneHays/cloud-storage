package com.waynehays.cloudfilestorage.service.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waynehays.cloudfilestorage.service.ratelimit.dto.RateLimitRule;
import com.waynehays.cloudfilestorage.service.ratelimit.dto.RequestData;
import io.github.bucket4j.Bucket;

import java.time.Duration;

public class BucketRegistry {
    private final Cache<RequestData, Bucket> buckets;

    public BucketRegistry(int bucketExpirationMinutes) {
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(bucketExpirationMinutes))
                .build();
    }

    public Bucket getOrCreateBucket(RequestData data, RateLimitRule rule) {
        return buckets.get(data, key -> Bucket.builder()
                .addLimit(limit -> limit.capacity(rule.capacity())
                        .refillGreedy(rule.tokensPerMinute(), Duration.ofMinutes(1)))
                .build());
    }

}
