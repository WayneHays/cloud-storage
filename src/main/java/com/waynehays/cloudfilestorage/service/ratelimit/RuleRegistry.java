package com.waynehays.cloudfilestorage.service.ratelimit;

import com.waynehays.cloudfilestorage.service.ratelimit.dto.RateLimitRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RuleRegistry {
    private final Map<String, RateLimitRule> cache;

    public RuleRegistry(List<RateLimitRule> rules) {
        this.cache = rules.stream()
                .collect(Collectors.toMap(
                        r -> r.endpoint() + "|" + r.httpMethod(),
                        r -> r
                ));
    }

    public Optional<RateLimitRule> getRule(String endpoint, String httpMethod) {
        return Optional.ofNullable(cache.get(endpoint + "|" + httpMethod));
    }
}
