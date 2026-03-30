package com.waynehays.cloudfilestorage.config.properties;

import com.waynehays.cloudfilestorage.service.ratelimiter.dto.RateLimitRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
        Duration bucketExpiration
) {
}
