package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "search")
public record SearchProperties(

        @NotNull(message = "Search limit must be set")
        @Size(min = 1, max = 100, message = "Search limit must be > 0 and <= 100")
        Integer limit
) {
}
