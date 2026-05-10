package com.waynehays.cloudfilestorage.files.operation.upload.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "executors")
record ExecutorProperties(

        @NotNull(message = "Upload thread pool size must be set")
        @Min(value = 1, message = "Upload thread pool size must be >= 1")
        @Max(value = 20, message = "Upload thread pool size must be <= 20")
        Integer uploadThreadPoolSize
) {
}
