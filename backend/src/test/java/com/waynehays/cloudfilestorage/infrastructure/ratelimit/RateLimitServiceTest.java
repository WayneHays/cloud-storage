package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.DisplayName;
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
    private RateLimitService service;

    private static final RequestData REQUEST = new RequestData(1L, "/api/resources", "GET");

    @Test
    @DisplayName("Should return unlimited when rule not found")
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
    @DisplayName("Should return allowed when token available")
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
    @DisplayName("Should return rejected when no tokens left")
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
