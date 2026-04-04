package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "cleanup.orphan-storage")
public record OrphanStorageCleanupProperties(

        @NotNull(message = "Interval for cleanup orphans from storage must be set")
        @Size(min = 10, max = 100, message = "Interval for cleanup orphans from storage must be >= 10 and <= 100")
        Duration interval,

        @NotNull(message = "Cleanup limit must be set")
        @Size(min = 10, max = 100, message = "Cleanup limit must be >= 10 and <= 100")
        Integer limit
) {
}
