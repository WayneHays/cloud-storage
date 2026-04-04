package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "cleanup.stale-deletion")
public record StaleDeletionCleanupProperties(

        @NotNull(message = "Interval for deleting stale entities must be set")
        @Size(min = 10, max = 100, message = "Interval for deleting stale entities must be >= 10 and <= 100")
        Duration interval,

        @NotNull(message = "Threshold for deleting stale entities must be set")
        @Size(min = 10, max = 100, message = "Threshold for deleting stale entities must be >= 10 and <= 100")
        Duration threshold
) {
}
