package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "archive")
public record ArchiveProperties(

        @NotNull(message = "Buffer size must be set")
        @Min(value = 1024, message = "Buffer size must be >= 1024")
        @Max(value = 8192, message = "Buffer size must be <= 8192")
        Integer bufferSize
) {
}
