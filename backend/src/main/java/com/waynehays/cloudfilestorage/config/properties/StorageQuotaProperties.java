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
@ConfigurationProperties(prefix = "storage-quota")
public record StorageQuotaProperties(

        @NotNull(message = "Default storage limit must be set")
        DataSize defaultLimit,

        @NotNull(message = "Reconciliation reconciliationInterval must be set")
        @DurationMin(minutes = 1, message = "Reconciliation reconciliationInterval must be >= 1m")
        @DurationMax(hours = 24, message = "Reconciliation reconciliationInterval must be <= 24h")
        Duration reconciliationInterval,

        @NotNull(message = "Reconciliation batch size must be set")
        @Min(value = 10, message = "Batch size must be >= 10")
        @Max(value = 1000, message = "Batch size must be <= 1000")
        Integer reconciliationBatchSize
) {
    private static final DataSize MIN_LIMIT = DataSize.ofMegabytes(100);
    private static final DataSize MAX_LIMIT = DataSize.ofGigabytes(10);

    @AssertTrue(message = "Default storage limit must be between 100 MB and 10 GB")
    boolean isDefaultLimitValid() {
        if (defaultLimit == null) {
            return true;
        }
        long bytes = defaultLimit.toBytes();
        return bytes >= MIN_LIMIT.toBytes() && bytes <= MAX_LIMIT.toBytes();
    }
}
