package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "cleanup.stale-deletion")
public record StaleDeletionCleanupProperties(

        @NotNull(message = "Interval for deleting stale entities must be set")
        @DurationMin(minutes = 1, message = "Stale deletion interval must be >= 1m")
        @DurationMax(hours = 24, message = "Stale deletion interval must be <= 24h")
        Duration interval,

        @NotNull(message = "Threshold for deleting stale entities must be set")
        @DurationMin(minutes = 5, message = "Stale deletion threshold must be >= 5m")
        @DurationMax(hours = 72, message = "Stale deletion threshold must be <= 72h")
        Duration threshold
) {
}
