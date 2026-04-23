package com.waynehays.cloudfilestorage.service.ratelimit;

import com.waynehays.cloudfilestorage.dto.internal.ratelimit.RateLimitCheckResult;
import com.waynehays.cloudfilestorage.dto.internal.ratelimit.RateLimitRule;
import com.waynehays.cloudfilestorage.dto.internal.ratelimit.RequestData;
import com.waynehays.cloudfilestorage.service.ratelimit.BucketRegistry;
import com.waynehays.cloudfilestorage.service.ratelimit.RateLimiterService;
import com.waynehays.cloudfilestorage.service.ratelimit.RuleRegistry;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RuleRegistry ruleRegistry;

    @Mock
    private BucketRegistry bucketRegistry;

    @InjectMocks
    private RateLimiterService service;

    private static final RequestData REQUEST = new RequestData(1L, "/api/resources", "GET");


    @Test
    void shouldReturnUnlimitedWhenNoRuleFound() {
        // given
        when(ruleRegistry.getRule(REQUEST.endpoint(), REQUEST.httpMethod()))
                .thenReturn(Optional.empty());

        // when
        RateLimitCheckResult result = service.checkRateLimit(REQUEST);

        // then
        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(-1);
        verifyNoInteractions(bucketRegistry);
    }

    @Test
    void shouldReturnAllowedWhenTokenAvailable() {
        // given
        RateLimitRule rule = new RateLimitRule("/api/resources", "GET", 10, 10);
        Bucket bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
                .build();

        when(ruleRegistry.getRule(REQUEST.endpoint(), REQUEST.httpMethod()))
                .thenReturn(Optional.of(rule));
        when(bucketRegistry.getOrCreateBucket(REQUEST, rule))
                .thenReturn(bucket);

        // when
        RateLimitCheckResult result = service.checkRateLimit(REQUEST);

        // then
        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(9);
    }

    @Test
    void shouldReturnRejectedWhenNoTokensLeft() {
        // given
        RateLimitRule rule = new RateLimitRule("/api/resources", "GET", 1, 1);
        Bucket bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(1).refillGreedy(1, Duration.ofMinutes(1)))
                .build();
        bucket.tryConsume(1);

        when(ruleRegistry.getRule(REQUEST.endpoint(), REQUEST.httpMethod()))
                .thenReturn(Optional.of(rule));
        when(bucketRegistry.getOrCreateBucket(REQUEST, rule))
                .thenReturn(bucket);

        // when
        RateLimitCheckResult result = service.checkRateLimit(REQUEST);

        // then
        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterSeconds()).isGreaterThanOrEqualTo(0);
        assertThat(result.errorMessage()).contains("/api/resources");
    }
}
