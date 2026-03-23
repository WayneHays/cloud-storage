package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cleanup")
public record CleanupProperties(

        @Size(min = 60000, max = 12000000, message = "Cleanup interval must be more than 60000 and less than 12000000")
        Long interval
) {
}
