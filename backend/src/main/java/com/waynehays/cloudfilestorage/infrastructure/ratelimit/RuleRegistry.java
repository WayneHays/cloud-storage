package com.waynehays.cloudfilestorage.infrastructure.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
class RuleRegistry {
    private static final String SEPARATOR = "|";
    private final Map<String, RateLimitRule> cache;

    RuleRegistry(RateLimitProperties properties) {
        this.cache = properties.rules()
                .stream()
                .collect(Collectors.toMap(
                        r -> r.endpoint() + SEPARATOR + r.httpMethod(),
                        r -> r
                ));
    }

    Optional<RateLimitRule> getRule(String endpoint, String httpMethod) {
        return Optional.ofNullable(cache.get(endpoint + SEPARATOR + httpMethod));
    }
}
