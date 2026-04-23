package com.waynehays.cloudfilestorage.resource.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "search")
public record SearchProperties(

        @NotNull(message = "Search limit must be set")
        @Min(value = 1, message = "Search limit must be >= 1")
        @Max(value = 100, message = "Search limit must be <= 100")
        Integer limit
) {
}
