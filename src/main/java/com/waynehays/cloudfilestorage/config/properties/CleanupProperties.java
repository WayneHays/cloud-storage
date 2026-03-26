package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cleanup")
public record CleanupProperties(

        @NotNull(message = "Cleanup interval must be set")
        @Min(value = 600, message = "Cleanup interval must be >= 600")
        @Max(value = 3600, message = "Cleanup interval must be <= 3600")
        Integer interval
) {
}
