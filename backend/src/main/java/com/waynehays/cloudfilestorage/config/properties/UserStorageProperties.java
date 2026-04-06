package com.waynehays.cloudfilestorage.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "user.storage")
public record UserStorageProperties(

        @NotNull(message = "Default storage limit must be set")
        DataSize defaultLimit,

        @NotNull(message = "Reconciliation interval must be set")
        @DurationMin(minutes = 1, message = "Reconciliation interval must be >= 1m")
        @DurationMax(hours = 24, message = "Reconciliation interval must be <= 24h")
        Duration reconciliationInterval,

        @NotNull(message = "Reconciliation batch size must be set")
        @Min(value = 10, message = "Buffer size must be >= 10")
        @Max(value = 1000, message = "Buffer size must be <= 1000")
        Integer reconciliationBatchSize
) {
    @AssertTrue(message = "Default storage limit must be between 1 MB and 100 GB")
    boolean isDefaultLimitValid() {
        if (defaultLimit == null) {
            return true;
        }
        long bytes = defaultLimit.toBytes();
        return bytes >= DataSize.ofMegabytes(1).toBytes()
                && bytes <= DataSize.ofGigabytes(100).toBytes();
    }
}
