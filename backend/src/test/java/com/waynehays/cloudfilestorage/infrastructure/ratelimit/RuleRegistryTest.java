package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleRegistryTest {

    @Test
    @DisplayName("Should return rule when exists")
    void shouldReturnRuleWhenExists() {
        // given
        RateLimitRule rule = new RateLimitRule("/api/resources", "GET", 10, 10);
        RateLimitProperties properties = mock(RateLimitProperties.class);
        when(properties.rules()).thenReturn(List.of(rule));
        RuleRegistry registry = new RuleRegistry(properties);

        // when
        Optional<RateLimitRule> result = registry.getRule("/api/resources", "GET");

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(rule);
    }

    @Test
    @DisplayName("Should return empty when rule not found")
    void shouldReturnEmptyWhenRuleNotFound() {
        // given
        RateLimitProperties properties = mock(RateLimitProperties.class);
        when(properties.rules()).thenReturn(List.of());
        RuleRegistry registry = new RuleRegistry(properties);

        // when
        Optional<RateLimitRule> result = registry.getRule("/api/unknown", "GET");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should distinguish by http method")
    void shouldDistinguishByHttpMethod() {
        // given
        RateLimitRule getRule = new RateLimitRule("/api/resources", "GET", 10, 10);
        RateLimitRule postRule = new RateLimitRule("/api/resources", "POST", 5, 5);
        RateLimitProperties properties = mock(RateLimitProperties.class);
        when(properties.rules()).thenReturn(List.of(getRule, postRule));
        RuleRegistry registry = new RuleRegistry(properties);

        // when
        Optional<RateLimitRule> getResult = registry.getRule("/api/resources", "GET");
        Optional<RateLimitRule> postResult = registry.getRule("/api/resources", "POST");

        // then
        assertThat(getResult.get().capacity()).isEqualTo(10);
        assertThat(postResult.get().capacity()).isEqualTo(5);
    }
}
