package com.waynehays.cloudfilestorage.config.properties;

import com.waynehays.cloudfilestorage.dto.internal.ratelimit.RateLimitRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(

        @NotNull(message = "Rate limit rules list must be set")
        List<@Valid RateLimitRule> rules,

        @NotNull(message = "Bucket expiration must be set")
        @DurationMin(minutes = 1, message = "Bucket expiration must be >= 1m")
        @DurationMax(hours = 1, message = "Bucket expiration must be <= 1h")
        Duration bucketExpiration
) {
    @AssertTrue(message = "Rate limit rules must be unique by endpoint + httpMethod")
    boolean isRuleUnique() {
        if (rules == null || rules.isEmpty()) {
            return true;
        }

        long distinctCount = rules.stream()
                .map(r -> r.endpoint() + ":" + r.httpMethod())
                .distinct()
                .count();
        return distinctCount == rules.size();
    }
}
