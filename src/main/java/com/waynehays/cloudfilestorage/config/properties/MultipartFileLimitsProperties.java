package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "minio.storage.limits")
public record MultipartFileLimitsProperties(

        @NotNull(message = "Max path length must be set")
        @Min(value = 1, message = "Max path length must be >= 1")
        @Max(value = 4096, message = "Max path length must be <= 4096")
        Integer maxPathLength,

        @NotNull(message = "Max filename length must be set")
        @Min(value = 1, message = "Max filename length must be >= 1")
        @Max(value = 255, message = "Max filename length must be <= 255")
        Integer maxFilenameLength
) {
}
