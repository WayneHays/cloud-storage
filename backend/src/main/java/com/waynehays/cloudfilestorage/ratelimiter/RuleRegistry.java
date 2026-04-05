package com.waynehays.cloudfilestorage.ratelimiter;

import com.waynehays.cloudfilestorage.config.properties.RateLimitProperties;
import com.waynehays.cloudfilestorage.ratelimiter.dto.RateLimitRule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RuleRegistry {
    private final Map<String, RateLimitRule> cache;

    public RuleRegistry(RateLimitProperties properties) {
        this.cache = properties.rules()
                .stream()
                .collect(Collectors.toMap(
                        r -> r.endpoint() + "|" + r.httpMethod(),
                        r -> r
                ));
    }

    public Optional<RateLimitRule> getRule(String endpoint, String httpMethod) {
        return Optional.ofNullable(cache.get(endpoint + "|" + httpMethod));
    }
}
