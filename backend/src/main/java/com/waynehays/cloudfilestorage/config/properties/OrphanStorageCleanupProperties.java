package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "cleanup.orphan-storage")
public record OrphanStorageCleanupProperties(

        @NotNull(message = "Interval for cleanup orphans from storage must be set")
        @DurationMin(minutes = 1, message = "Cleanup interval must be >= 1m")
        @DurationMax(hours = 24, message = "Cleanup interval must be <= 24h")
        Duration interval,

        @NotNull(message = "Cleanup limit must be set")
        @Min(value = 10, message = "Cleanup limit must be >= 10")
        @Max(value = 100, message = "Cleanup limit must be <= 100")
        Integer limit
) {
}
