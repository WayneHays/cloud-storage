package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Should create new bucket for new request")
    void shouldCreateNewBucketForNewRequest() {
        // when
        Bucket bucket = bucketRegistry.getOrCreateBucket(REQUEST, RULE);

        // then
        assertThat(bucket).isNotNull();
        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should return same bucket for same request")
    void shouldReturnSameBucketForSameRequest() {
        // when
        Bucket first = bucketRegistry.getOrCreateBucket(REQUEST, RULE);
        Bucket second = bucketRegistry.getOrCreateBucket(REQUEST, RULE);

        // then
        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("Should create different buckets for different requests")
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
    @DisplayName("Should create bucket with correct capacity")
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
