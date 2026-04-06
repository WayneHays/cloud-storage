package com.waynehays.cloudfilestorage.config.properties;

import com.waynehays.cloudfilestorage.ratelimiter.dto.RateLimitRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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

        @NotEmpty(message = "Rate limit rules list must be set")
        List<@Valid RateLimitRule> rules,

        @NotNull(message = "Bucket expiration must be set")
        @DurationMin(minutes = 1, message = "Bucket expiration must be >= 1m")
        @DurationMax(hours = 24, message = "Bucket expiration must be <= 24h")
        Duration bucketExpiration
) {
}
