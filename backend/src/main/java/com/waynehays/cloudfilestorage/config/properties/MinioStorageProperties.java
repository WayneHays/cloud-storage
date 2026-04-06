package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "minio.storage")
public record MinioStorageProperties(

        @NotBlank(message = "Bucket name must be set")
        @Length(max = 50, message = "Bucket name must be <= 50 characters")
        String bucketName,

        @NotNull(message = "Batch size must be set")
        @Min(value = 3, message = "Batch size must be >= 3")
        @Max(value = 1000, message = "Batch size must be <= 1000")
        Integer deletionBatchSize,

        @NotNull(message = "Connect timeout must be set")
        @DurationMin(seconds = 1, message = "Connect timeout must be >= 1s")
        @DurationMax(seconds = 30, message = "Connect timeout must be <= 30s")
        Duration connectTimeout,

        @NotNull(message = "Read timeout must be set")
        @DurationMin(seconds = 5, message = "Read timeout must be >= 5s")
        @DurationMax(minutes = 5, message = "Read timeout must be <= 5m")
        Duration readTimeout,

        @NotNull(message = "Write timeout must be set")
        @DurationMin(seconds = 5, message = "Write timeout must be >= 5s")
        @DurationMax(minutes = 10, message = "Write timeout must be <= 10m")
        Duration writeTimeout,

        @NotNull(message = "Call timeout must be set")
        @DurationMin(seconds = 10, message = "Call timeout must be >= 10s")
        @DurationMax(minutes = 15, message = "Call timeout must be <= 15m")
        Duration callTimeout
) {
}
