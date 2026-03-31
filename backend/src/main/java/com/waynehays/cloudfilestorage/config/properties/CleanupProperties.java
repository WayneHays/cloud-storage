package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "orphan-cleanup")
public record CleanupProperties(

        @NotNull(message = "Cleanup interval must be set")
        Duration interval
) {
}
