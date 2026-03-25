package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cleanup")
public record CleanupProperties(

        @Size(min = 60, max = 180, message = "Cleanup interval must be more than 60 and less than 180")
        Long interval
) {
}
