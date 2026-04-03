package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "executors")
public record ExecutorProperties(

        @NotNull(message = "Move thread pool size must be set")
        @Min(value = 1, message = "Move thread pool size must be > 0")
        @Max(value = 20, message = "Move thread pool size must be <= 20")
        Integer moveThreadPoolSize,

        @NotNull(message = "Upload thread pool size must be set")
        @Min(value = 1, message = "Upload thread pool size must be > 0")
        @Max(value = 20, message = "Upload thread pool size must be <= 20")
        Integer uploadThreadPoolSize
) {
}
