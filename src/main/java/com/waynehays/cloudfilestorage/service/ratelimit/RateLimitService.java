package com.waynehays.cloudfilestorage.service.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(Long userId) {
        return buckets.computeIfAbsent(userId, this::createBucket);
    }

    private Bucket createBucket(Long userId) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(20)
                        .refillGreedy(20, Duration.ofMinutes(1)))
                .build();
    }
}
