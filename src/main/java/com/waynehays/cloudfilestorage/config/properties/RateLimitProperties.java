package com.waynehays.cloudfilestorage.config.properties;

import com.waynehays.cloudfilestorage.service.ratelimit.dto.RateLimitRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(

        @NotEmpty(message = "Rate limit rules list cannot be empty")
        List<@Valid RateLimitRule> rules,

        @NotNull(message = "Bucket expiration must not be null")
        @Min(value = 1, message = "Bucket expiration must be at least 1 minute")
        Integer bucketExpirationMinutes
) {
}
