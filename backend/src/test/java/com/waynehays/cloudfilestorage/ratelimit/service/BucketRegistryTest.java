package com.waynehays.cloudfilestorage.ratelimit.service;

import com.waynehays.cloudfilestorage.ratelimit.config.RateLimitProperties;
import com.waynehays.cloudfilestorage.ratelimit.dto.RateLimitRule;
import com.waynehays.cloudfilestorage.ratelimit.dto.RequestData;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BucketRegistryTest {

    private BucketRegistry bucketRegistry;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = mock(RateLimitProperties.class);
        when(properties.bucketExpiration()).thenReturn(Duration.ofMinutes(10));
        bucketRegistry = new BucketRegistry(properties);
    }

    private static final RateLimitRule RULE = new RateLimitRule("/api/resources", "GET", 10, 10);
    private static final RequestData REQUEST = new RequestData(1L, "/api/resources", "GET");

    @Test
    void shouldCreateNewBucketForNewRequest() {
        // when
        Bucket bucket = bucketRegistry.getOrCreateBucket(REQUEST, RULE);

        // then
        assertThat(bucket).isNotNull();
        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
    }

    @Test
    void shouldReturnSameBucketForSameRequest() {
        // when
        Bucket first = bucketRegistry.getOrCreateBucket(REQUEST, RULE);
        Bucket second = bucketRegistry.getOrCreateBucket(REQUEST, RULE);

        // then
        assertThat(first).isSameAs(second);
    }

    @Test
    void shouldCreateDifferentBucketsForDifferentRequests() {
        // given
        RequestData anotherRequest = new RequestData(2L, "/api/directories", "POST");

        // when
        Bucket first = bucketRegistry.getOrCreateBucket(REQUEST, RULE);
        Bucket second = bucketRegistry.getOrCreateBucket(anotherRequest, RULE);

        // then
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void shouldCreateBucketWithCorrectCapacity() {
        // given
        RateLimitRule customRule = new RateLimitRule("/api/upload", "POST", 5, 5);
        RequestData request = new RequestData(1L, "/api/upload", "POST");

        // when
        Bucket bucket = bucketRegistry.getOrCreateBucket(request, customRule);

        // then
        assertThat(bucket.getAvailableTokens()).isEqualTo(5);
    }
}
