package com.waynehays.cloudfilestorage.core.quota.reconciliation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "reconciliation")
record StorageQuotaReconciliationProperties(

        @NotNull(message = "Reconciliation reconciliationInterval must be set")
        @DurationMin(minutes = 1, message = "Reconciliation reconciliationInterval must be >= 1m")
        @DurationMax(hours = 24, message = "Reconciliation reconciliationInterval must be <= 24h")
        Duration interval,

        @NotNull(message = "Reconciliation batch size must be set")
        @Min(value = 10, message = "Batch size must be >= 10")
        @Max(value = 1000, message = "Batch size must be <= 1000")
        Integer batchSize
) {
}
